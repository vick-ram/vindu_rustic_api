package org.example.data.repo

import io.celery.CeleryApp
import org.example.config.security.JwtConfig
import org.example.config.security.OtpService
import org.example.config.security.OtpVerificationResult
import org.example.config.security.PasswordHasher
import org.example.config.security.TokenService
import org.example.data.db.entities.UserEntity
import org.example.data.db.tables.Users
import org.example.data.mappers.UserMapper
import org.example.domain.models.identity.Session
import org.example.domain.models.identity.TokenResponse
import org.example.domain.models.identity.User
import org.example.domain.repo.SessionRepository
import org.example.domain.repo.UserRepository
import org.example.plugins.AuthenticationException
import org.example.plugins.NotFoundException
import org.example.plugins.TokenExpiredException
import org.example.utils.customMatch
import org.example.utils.suspendTransaction
import java.time.OffsetDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import org.example.config.AppConfig
import org.example.config.security.OtpPurpose
import org.example.data.mappers.NotificationMapper
import org.example.domain.models.NotificationChannel
import org.example.domain.models.system.DispatchResult
import org.example.domain.models.system.Notification
import org.example.domain.repo.DeviceTokenRepository
import org.example.utils.notifications.DatabaseNotificationService
import org.example.utils.notifications.EmailNotificationService
import org.example.utils.notifications.FcmNotificationService
import org.example.utils.notifications.NotificationDispatcher
import org.example.utils.notifications.SMSService
import org.slf4j.LoggerFactory
import java.net.InetAddress

class UserRepositoryImpl(
    private val userMapper: UserMapper,
    private val notificationMapper: NotificationMapper,
    private val jwtConfig: JwtConfig,
    private val appConfig: AppConfig,
    private val tokenService: TokenService,
    private val sessionRepository: SessionRepository,
    private val deviceTokenRepository: DeviceTokenRepository,
    private val otpService: OtpService,
    private val notificationAdapter: NotificationServiceAdapter,
    private val json: Json
) :
    CrudRepositoryImpl<UserEntity, User>(UserEntity, User::class),
    UserRepository {

    private val celeryApp = CeleryApp(name = "notification")
    private val notificationDispatcher = NotificationDispatcher(
        services = mapOf(
            NotificationChannel.EMAIL to EmailNotificationService(appConfig),
            NotificationChannel.SMS to SMSService(appConfig),
            NotificationChannel.DATABASE to DatabaseNotificationService(notificationMapper),
            NotificationChannel.FCM to FcmNotificationService(appConfig, deviceTokenRepository)
        )
    )

    override suspend fun create(entity: User): User {
        val hashedPassword = PasswordHasher.hash(entity.password)
        val newUser = entity.copy(password = hashedPassword)

        // send verification OTPs
        launchOTPVerification(newUser)

        return super.create(newUser)
    }

    override suspend fun searchUsers(
        query: String,
        offset: Int,
        limit: Int
    ): List<User> = suspendTransaction {
        UserEntity.find { Users.tsv.customMatch(query) }
            .offset(offset.toLong())
            .limit(limit)
            .map { it.toDomain() }
    }

    override suspend fun findByEmail(email: String): User? = suspendTransaction {
        UserEntity.find { Users.email.eq(email) }
            .firstOrNull()
            ?.toDomain()
    }

    override suspend fun updateEmailVerification(userId: String, verified: Boolean) = suspendTransaction {
        UserEntity.findByIdAndUpdate(userId) { user ->
            user.emailVerified = verified
            user.updatedAt = OffsetDateTime.now()
        }
        return@suspendTransaction
    }

    override suspend fun updatePhoneVerification(userId: String, verified: Boolean) = suspendTransaction {
        UserEntity.findByIdAndUpdate(userId) { user ->
            user.phoneVerified = verified
            user.updatedAt = OffsetDateTime.now()
        }
        return@suspendTransaction
    }

    override suspend fun updatePassword(userId: String, oldPassword: String, newPassword: String) = suspendTransaction {
        val user = read(userId) ?: throw NotFoundException("User not found")
        val hashedNewPassword = PasswordHasher.hash(newPassword)

        if (!PasswordHasher.verify(hashedNewPassword, user.password)) {
            // Password do not match
        }

        UserEntity.findByIdAndUpdate(userId) {
            it.password = hashedNewPassword
        }
        return@suspendTransaction
    }

    override suspend fun update2FAStatus(userId: String, enabled: Boolean) = suspendTransaction {
        UserEntity.findByIdAndUpdate(userId) {
            it.enable2FA = enabled
        }
        return@suspendTransaction
    }

    override fun UserEntity.toDomain(): User = userMapper.toModel(this)

    override fun User.toEntity(entity: UserEntity) {
        userMapper.toEntity(this, entity)
    }

    override fun getId(domain: User): String = domain.id

    override suspend fun login(
        email: String,
        password: String,
        ipAddress: InetAddress?,
        deviceInfo: String?
    ): TokenResponse = suspendTransaction {
        val user = findByEmail(email) ?: throw NotFoundException("user not found")

        if (!PasswordHasher.verify(password, user.password)) {
            // Send security alert for failed login
            notificationAdapter.sendSecurityAlert(
                userId = user.id,
                alertType = "failed_login_attempt",
                details = mapOf(
                    "ipAddress" to (ipAddress?.hostAddress ?: "Unknown"),
                    "deviceInfo" to (deviceInfo ?: "Unknown"),
                    "timestamp" to OffsetDateTime.now().toString()
                )
            )
            throw AuthenticationException("Invalid email or password")
        }
        // update last login
        update(user.id, user.copy(lastLoginAt = OffsetDateTime.now()))
        // Send login alert
        notificationAdapter.sendLoginAlert(
            userId = user.id,
            ipAddress = ipAddress?.hostAddress,
            deviceInfo = deviceInfo
        )

        // Check if 2FA is enabled
        if (user.twoFactorEnabled) {
            val otp = otpService.generateOtp(user.id, OtpPurpose.LOGIN_2FA)
            notificationAdapter.sendOtpNotification(
                userId = user.id,
                otp = otp,
                purpose = "two_factor_auth",
                channels = get2FAChannels(user)
            )
            throw TwoFactorRequiredException(user.id)
        }

        generateTokens(user, ipAddress, deviceInfo)
    }

    private fun get2FAChannels(user: User): List<NotificationChannel> {
        val channels = mutableListOf<NotificationChannel>()
        channels.add(NotificationChannel.EMAIL)
        if (user.phoneNumber != null) {
            channels.add(NotificationChannel.SMS)
        }
        channels.add(NotificationChannel.DATABASE)
        return channels
    }

    suspend fun verify2FAAndLogin(
        userId: String,
        otp: String,
        ipAddress: InetAddress? = null,
        deviceInfo: String? = null
    ): TokenResponse = suspendTransaction {
        when (val result = otpService.verifyOtp(userId, OtpPurpose.LOGIN_2FA, otp)) {
            is OtpVerificationResult.Success -> {
                val user = read(userId)
                    ?: throw NotFoundException("user not found")
                generateTokens(user, ipAddress, deviceInfo)
            }
            is OtpVerificationResult.Expired -> {
                throw OtpExpiredException()
            }
            is OtpVerificationResult.Invalid -> {
                throw InvalidOtpException(result.remainingAttempts)
            }
            is OtpVerificationResult.TooManyAttempts -> {
                // Security alert for too many failed 2FA attempts
                notificationAdapter.sendSecurityAlert(
                    userId = userId,
                    alertType = "too_many_2fa_attempts",
                    details = mapOf(
                        "attempts" to "max",
                        "ipAddress" to (ipAddress?.hostAddress ?: "Unknown"),
                        "timestamp" to OffsetDateTime.now().toString()
                    )
                )
                throw TooManyAttemptsException()
            }
        }
    }

    suspend fun refreshTokens(refreshToken: String) : TokenResponse = suspendTransaction {
        val session = sessionRepository.findByRefreshToken(refreshToken)
            ?: throw NotFoundException("session not found")

        if (session.expiresAt.isBefore(OffsetDateTime.now())) {
            throw TokenExpiredException("token expired", session.expiresAt)
        }

        val user = read(session.userId)
            ?: throw NotFoundException("user not found")

        // Invalidate old refresh token
        sessionRepository.delete(session.id)

        // Generate new tokens
        val newAccessToken = jwtConfig.generateAccessToken(user.id, user.email)
        val newRefreshToken = jwtConfig.generateRefreshToken(user.id)

        // Store new session
        sessionRepository.create(
            Session(
                userId = user.id,
                refreshToken = newRefreshToken,
                expiresAt = OffsetDateTime.now().plusDays(7),
            )
        )

        TokenResponse("bearer", newAccessToken, newRefreshToken)
    }

    override suspend fun logout(userId: String, accessToken: String): Boolean  = suspendTransaction{
        // Blacklist access token
        tokenService.blacklistToken(
            accessToken,
            appConfig.security.accessExpiry
        )

        // invalidate all refresh tokens
        tokenService.invalidateAllUserTokens(userId)
        sessionRepository.deleteAllForUser(userId)
        true
    }

    private suspend fun launchOTPVerification(user: User) {
        // Send email verification OTP
        if (!user.emailVerified) {
            val emailOtp = otpService.generateOtp(user.id, OtpPurpose.EMAIL_VERIFICATION)
            notificationAdapter.sendOtpNotification(
                userId = user.id,
                otp = emailOtp,
                purpose = "email_verification",
                channels = listOf(NotificationChannel.EMAIL)
            )
        }

        // Send phone verification OTP if phone exists
        if (user.phoneNumber != null && !user.phoneVerified) {
            val phoneOtp = otpService.generateOtp(user.id, OtpPurpose.PHONE_VERIFICATION)
            notificationAdapter.sendOtpNotification(
                userId = user.id,
                otp = phoneOtp,
                purpose = "phone_verification",
                channels = listOf(NotificationChannel.SMS)
            )
        }
    }

    override suspend fun verifyEmail(userId: String, otp: String): VerificationResult {
        when (val result = otpService.verifyOtp(userId, OtpPurpose.EMAIL_VERIFICATION, otp)) {
            is OtpVerificationResult.Success -> {
                updateEmailVerification(userId, verified = true)
                return VerificationResult.Success("Email verified successfully")
            }
            is OtpVerificationResult.Expired -> {
                return VerificationResult.Failure("OTP has expired")
            }
            is OtpVerificationResult.Invalid -> {
                return VerificationResult.Failure(
                    "Invalid OTP. ${result.remainingAttempts} attempts remaining"
                )
            }
            is OtpVerificationResult.TooManyAttempts -> {
                return VerificationResult.Failure(
                    "Too many attempts. Please request a new OTP"
                )
            }
        }
    }

    override suspend fun verifyPhone(userId: String, otp: String): VerificationResult {
        when (val result = otpService.verifyOtp(userId, OtpPurpose.PHONE_VERIFICATION, otp)) {
            is OtpVerificationResult.Success -> {
                updatePhoneVerification(userId, verified = true)
                return VerificationResult.Success("Phone verified successfully")
            }
            is OtpVerificationResult.Expired -> {
                return VerificationResult.Failure("OTP has expired")
            }
            is OtpVerificationResult.Invalid -> {
                return VerificationResult.Failure(
                    "Invalid OTP. ${result.remainingAttempts} attempts remaining"
                )
            }
            is OtpVerificationResult.TooManyAttempts -> {
                return VerificationResult.Failure(
                    "Too many attempts. Please request a new OTP"
                )
            }
        }
    }

    override suspend fun resendOtp(userId: String, purpose: OtpPurpose): ResendResult = suspendTransaction {
        val user = read(userId) ?: return@suspendTransaction ResendResult.UserNotFound

        if (!otpService.canResendOtp(userId, purpose)) {
            ResendResult.Cooldown(60) // seconds
        }

        val otp = otpService.generateOtp(userId, purpose)

        val channels = when (purpose) {
            OtpPurpose.EMAIL_VERIFICATION -> listOf(NotificationChannel.EMAIL)
            OtpPurpose.PHONE_VERIFICATION -> listOf(NotificationChannel.SMS)
            OtpPurpose.PASSWORD_RESET -> listOf(NotificationChannel.EMAIL)
            OtpPurpose.LOGIN_2FA -> get2FAChannels(user)
            else -> listOf(NotificationChannel.EMAIL)
        }

        notificationAdapter.sendOtpNotification(
            userId = userId,
            otp = otp,
            purpose = purpose.name.lowercase(),
            channels = channels
        )

        ResendResult.Success
    }

    override suspend fun requestPasswordReset(email: String) {
        val user = findByEmail(email) ?: return // Don't reveal if user exists
        val otp = otpService.generateOtp(user.id, OtpPurpose.PASSWORD_RESET)
//        notificationAdapter.sendOtpNotification(
//            userId = user.id,
//            purpose = "PASSWORD_RESET",
//            channels = listOf(NotificationChannel.EMAIL)
//        )
    }

    override suspend fun resetPassword(email: String, otp: String, newPassword: String): ResetPasswordResult {
        val user = findByEmail(email) ?: return ResetPasswordResult.UserNotFound

        when (val result = otpService.verifyOtp(user.id, OtpPurpose.PASSWORD_RESET, otp)) {
            is OtpVerificationResult.Success -> {
                val hashedPassword = PasswordHasher.hash(newPassword)
                updatePassword(user.id, user.password,hashedPassword)
                // Invalidate all existing sessions
                tokenService.invalidateAllUserTokens(user.id)
                sessionRepository.deleteAllForUser(user.id)
                return ResetPasswordResult.Success
            }
            is OtpVerificationResult.Expired -> {
                return ResetPasswordResult.Failure("OTP has expired")
            }
            is OtpVerificationResult.Invalid -> {
                return ResetPasswordResult.Failure(
                    "Invalid OTP. ${result.remainingAttempts} attempts remaining"
                )
            }
            is OtpVerificationResult.TooManyAttempts -> {
                return ResetPasswordResult.Failure("Too many attempts. Please request a new OTP")
            }
        }
    }

    override suspend fun verify2FA(userId: String, otp: String): VerificationResult {
        return when (val result = otpService.verifyOtp(userId, OtpPurpose.LOGIN_2FA, otp)) {
            is OtpVerificationResult.Success -> VerificationResult.Success("2FA verified successfully")
            is OtpVerificationResult.Expired -> VerificationResult.Failure("OTP has expired")
            is OtpVerificationResult.Invalid -> {
                VerificationResult.Failure("Invalid OTP. ${result.remainingAttempts} attempts remaining")
            }
            is OtpVerificationResult.TooManyAttempts -> {
                VerificationResult.Failure("Too many attempts. Please request a new OTP")
            }
        }
    }

    private suspend fun generateTokens(user: User, ipAddress: InetAddress? = null, deviceInfo: String? = null): TokenResponse {
        val accessToken = jwtConfig.generateAccessToken(user.id, user.email)
        val refreshToken = jwtConfig.generateRefreshToken(user.id)

        sessionRepository.create(
            Session(
                userId = user.id,
                refreshToken = refreshToken,
                ipAddress = ipAddress,
                userAgent = deviceInfo,
                expiresAt = OffsetDateTime.now().plusDays(7),
            )
        )

        return TokenResponse("bearer", accessToken, refreshToken)
    }
}


@Serializable
data class SendVerificationRequest(
    val type: String // "email" or "phone"
)

@Serializable
data class VerifyOtpRequest(
    val otp: String
)

@Serializable
data class ForgotPasswordRequest(
    val email: String
)

@Serializable
data class ResetPasswordRequest(
    val email: String,
    val otp: String,
    val newPassword: String
)

sealed class VerificationResult {
    data class Success(val message: String) : VerificationResult()
    data class Failure(val message: String) : VerificationResult()
}

sealed class ResendResult {
    object Success : ResendResult()
    data class Cooldown(val seconds: Long) : ResendResult()
    object UserNotFound : ResendResult()
}

sealed class ResetPasswordResult {
    object Success : ResetPasswordResult()
    data class Failure(val message: String) : ResetPasswordResult()
    object UserNotFound : ResetPasswordResult()
}

sealed class Enable2FAResult {
    data class Success(val message: String) : Enable2FAResult()
    object UserNotFound : Enable2FAResult()
}

class TwoFactorRequiredException(val userId: String) : RuntimeException("2FA verification required")
class OtpExpiredException : RuntimeException("OTP has expired")
class InvalidOtpException(val remainingAttempts: Int) : RuntimeException("Invalid OTP. $remainingAttempts attempts remaining")
class TooManyAttemptsException : RuntimeException("Too many attempts. Please request a new OTP")


// NotificationTemplates.kt
object NotificationTemplates {
    const val EMAIL_VERIFICATION = "email_verification"
    const val PHONE_VERIFICATION = "phone_verification"
    const val PASSWORD_RESET = "password_reset"
    const val WELCOME = "welcome"
    const val LOGIN_ALERT = "login_alert"
    const val ACCOUNT_LOCKED = "account_locked"
    const val TWO_FACTOR_AUTH = "two_factor_auth"
    const val SESSION_EXPIRED = "session_expired"
    const val DEVICE_VERIFICATION = "device_verification"

    fun createOtpNotification(
        userId: String,
        channel: NotificationChannel,
        otp: String,
        purpose: String,
        expiresInMinutes: Int = 5
    ): Notification {
        return Notification(
            userId = userId,
            channel = channel,
            type = when (purpose) {
                "email_verification" -> EMAIL_VERIFICATION
                "phone_verification" -> PHONE_VERIFICATION
                "password_reset" -> PASSWORD_RESET
                "two_factor_auth" -> TWO_FACTOR_AUTH
                else -> EMAIL_VERIFICATION
            },
            title = "otp_verification",
            metadata = mapOf(
                "otp" to otp,
                "purpose" to purpose,
                "expiresInMinutes" to expiresInMinutes.toString()
            )
        )
    }

    fun createWelcomeNotification(userId: String, userName: String): Notification {
        return Notification(
            userId = userId,
            channel = NotificationChannel.EMAIL,
            title = "welcome",
            type = WELCOME,
            metadata = mapOf(
                "userName" to userName,
                "loginUrl" to "https://yourapp.com/login"
            )
        )
    }

    fun createLoginAlertNotification(
        userId: String,
        ipAddress: String,
        deviceInfo: String,
        timestamp: OffsetDateTime
    ): Notification {
        return Notification(
            userId = userId,
            channel = NotificationChannel.EMAIL,
            type = LOGIN_ALERT,
            title = "Login Alert",
            metadata = mapOf(
                "ipAddress" to ipAddress,
                "deviceInfo" to deviceInfo,
                "timestamp" to timestamp.toString(),
                "ifNotYouUrl" to "https://yourapp.com/security"
            ),
        )
    }
}

// NotificationServiceAdapter.kt
class NotificationServiceAdapter(
    private val celeryApp: CeleryApp,
    private val json: Json
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun sendOtpNotification(
        userId: String,
        otp: String,
        purpose: String,
        channels: List<NotificationChannel> = listOf(NotificationChannel.EMAIL)
    ): Result<Map<NotificationChannel, DispatchResult>> {
        return try {
            val notifications = channels.map { channel ->
                NotificationTemplates.createOtpNotification(
                    userId = userId,
                    channel = channel,
                    otp = otp,
                    purpose = purpose
                )
            }

            // Send via Celery for async processing
            val results = notifications.map { notification ->
                val taskResult = celeryApp.sendTask(
                    taskName = "dispatch_notification",
                    kwargs = mapOf(
                        "notification" to json.encodeToJsonElement(notification)
                    )
                )
                notification.channel to taskResult
            }

            // If multiple channels, use multi-channel dispatch
            if (channels.size > 1) {
                val multiResult = celeryApp.sendTask(
                    taskName = "dispatch_multi_channel_notification",
                    kwargs = mapOf(
                        "notification" to json.encodeToJsonElement(notifications.first()),
                        "channels" to json.encodeToJsonElement(channels)
                    )
                )
                logger.info("Multi-channel dispatch queued: ${notifications.first().id}")
            }

            Result.success(
                results.associate { (channel, _) ->
                    channel to DispatchResult(
                        notificationId = notifications.first().id,
                        channel = channel,
                        success = true,
                        message = "Notification queued successfully"
                    )
                }
            )
        } catch (e: Exception) {
            logger.error("Failed to queue OTP notification", e)
            Result.failure(e)
        }
    }

    suspend fun sendWelcomeNotification(userId: String, userName: String): Boolean {
        return try {
            val notification = NotificationTemplates.createWelcomeNotification(userId, userName)
            celeryApp.sendTask(
                taskName = "dispatch_notification",
                kwargs = mapOf(
                    "notification" to json.encodeToJsonElement(notification)
                )
            )
            true
        } catch (e: Exception) {
            logger.error("Failed to send welcome notification", e)
            false
        }
    }

    suspend fun sendLoginAlert(
        userId: String,
        ipAddress: String?,
        deviceInfo: String?
    ): Boolean {
        return try {
            val notification = NotificationTemplates.createLoginAlertNotification(
                userId = userId,
                ipAddress = ipAddress ?: "Unknown",
                deviceInfo = deviceInfo ?: "Unknown",
                timestamp = OffsetDateTime.now()
            )

            // High priority - send to multiple channels
            celeryApp.sendTask(
                taskName = "dispatch_multi_channel_notification",
                kwargs = mapOf(
                    "notification" to json.encodeToJsonElement(notification),
                    "channels" to json.encodeToJsonElement(
                        listOf(NotificationChannel.EMAIL, NotificationChannel.DATABASE)
                    )
                ),
                priority = 1 // High priority
            )
            true
        } catch (e: Exception) {
            logger.error("Failed to send login alert", e)
            false
        }
    }

    suspend fun sendSecurityAlert(
        userId: String,
        alertType: String,
        details: Map<String, String>
    ): Boolean {
        return try {
            val notification = Notification(
                userId = userId,
                channel = NotificationChannel.EMAIL,
                title = "security_alert",
                type = alertType,
                metadata = details,
            )

            celeryApp.sendTask(
                taskName = "dispatch_multi_channel_notification",
                kwargs = mapOf(
                    "notification" to json.encodeToJsonElement(notification),
                    "channels" to json.encodeToJsonElement(
                        listOf(
                            NotificationChannel.EMAIL,
                            NotificationChannel.SMS,
                            NotificationChannel.DATABASE
                        )
                    )
                ),
                priority = 2 // Critical priority
            )
            true
        } catch (e: Exception) {
            logger.error("Failed to send security alert", e)
            false
        }
    }
}
