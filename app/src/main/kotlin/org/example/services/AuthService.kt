package org.example.services

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.example.config.AppConfig
import org.example.config.security.JwtConfig
import org.example.config.security.PasswordHasher
import org.example.data.repo.SessionRepository
import org.example.data.repo.UserRepository
import org.example.di.Component
import org.example.di.Inject
import org.example.domain.models.identity.Session
import org.example.domain.models.identity.TokenResponse
import org.example.domain.models.identity.User
import org.example.domain.repo.NotificationAdapter
import org.example.exceptions.*
import java.net.InetAddress
import java.time.OffsetDateTime

@Component
class AuthService @Inject constructor(
    private val userRepository: UserRepository,
    private val jwtConfig: JwtConfig,
    private val appConfig: AppConfig,
    private val tokenService: TokenService,
    private val sessionRepository: SessionRepository,
    private val otpService: OtpService,
    private val notificationAdapter: NotificationAdapter
) {
    suspend fun updatePassword(userId: String, oldPassword: String, newPassword: String) {
        val user = userRepository.read(userId) ?: throw NotFoundException("User not found")

        if (!PasswordHasher.verify(oldPassword, user.password)) {
            throw AuthenticationException("Old password does not match")
        }

        userRepository.updatePassword(userId, PasswordHasher.hash(newPassword))
    }

    suspend fun login(
        email: String,
        password: String,
        ipAddress: InetAddress?,
        deviceInfo: String?
    ): TokenResponse {
        val user = userRepository.findByEmail(email) ?: throw NotFoundException("user not found")

        if (!PasswordHasher.verify(password, user.password)) {
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

        userRepository.update(user.id, user.copy(lastLoginAt = OffsetDateTime.now()))

        notificationAdapter.sendLoginAlert(
            userId = user.id,
            ipAddress = ipAddress?.hostAddress,
            deviceInfo = deviceInfo
        )

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

        return generateTokens(user, ipAddress, deviceInfo)
    }

    private fun get2FAChannels(user: User): List<String> {
        val channels = mutableListOf<String>()
        channels.add("email")
        if (user.phoneNumber != null) {
            channels.add("sms")
        }
        channels.add("database")
        return channels
    }

    suspend fun verify2FAAndLogin(
        userId: String,
        otp: String,
        ipAddress: String? = null,
        deviceInfo: String? = null
    ): TokenResponse {
        when (val result = otpService.verifyOtp(userId, OtpPurpose.LOGIN_2FA, otp)) {
            is OtpVerificationResult.Success -> {
                val user = userRepository.read(userId)
                    ?: throw NotFoundException("user not found")
                return generateTokens(user, withContext(Dispatchers.IO) {
                    InetAddress.getByName(ipAddress)
                }, deviceInfo)
            }
            is OtpVerificationResult.Expired -> {
                throw OtpExpiredException()
            }
            is OtpVerificationResult.Invalid -> {
                throw InvalidOtpException(result.remainingAttempts)
            }
            is OtpVerificationResult.TooManyAttempts -> {
                notificationAdapter.sendSecurityAlert(
                    userId = userId,
                    alertType = "too_many_2fa_attempts",
                    details = mapOf(
                        "attempts" to "max",
                        "ipAddress" to (ipAddress ?: "Unknown"),
                        "timestamp" to OffsetDateTime.now().toString()
                    )
                )
                throw TooManyAttemptsException()
            }
        }
    }

    suspend fun refreshTokens(refreshToken: String): TokenResponse {
        val session = sessionRepository.findByRefreshToken(refreshToken)
            ?: throw NotFoundException("session not found")

        if (session.expiresAt.isBefore(OffsetDateTime.now())) {
            throw TokenExpiredException("token expired", session.expiresAt)
        }

        val user = userRepository.read(session.userId)
            ?: throw NotFoundException("user not found")

        sessionRepository.delete(session.id)

        val newAccessToken = jwtConfig.generateAccessToken(user.id, user.email)
        val newRefreshToken = jwtConfig.generateRefreshToken(user.id)

        sessionRepository.create(
            Session(
                userId = user.id,
                refreshToken = newRefreshToken,
                expiresAt = OffsetDateTime.now().plusDays(7),
            )
        )

        return TokenResponse("bearer", newAccessToken, newRefreshToken)
    }

    suspend fun logout(userId: String, accessToken: String): Boolean {
        tokenService.blacklistToken(
            accessToken,
            appConfig.security.accessExpiry
        )
        tokenService.invalidateAllUserTokens(userId)
        sessionRepository.deleteAllForUser(userId)
        return true
    }

    suspend fun launchOTPVerification(user: User) {
        if (!user.emailVerified) {
            val emailOtp = otpService.generateOtp(user.id, OtpPurpose.EMAIL_VERIFICATION)
            notificationAdapter.sendOtpNotification(
                userId = user.id,
                otp = emailOtp,
                purpose = "email_verification",
                channels = listOf("email")
            )
        }

        if (user.phoneNumber != null && !user.phoneVerified) {
            val phoneOtp = otpService.generateOtp(user.id, OtpPurpose.PHONE_VERIFICATION)
            notificationAdapter.sendOtpNotification(
                userId = user.id,
                otp = phoneOtp,
                purpose = "phone_verification",
                channels = listOf("sms")
            )
        }
    }

    suspend fun verifyEmail(userId: String, otp: String): VerificationResult {
        return when (val result = otpService.verifyOtp(userId, OtpPurpose.EMAIL_VERIFICATION, otp)) {
            is OtpVerificationResult.Success -> {
                userRepository.updateEmailVerification(userId, verified = true)
                VerificationResult.Success("Email verified successfully")
            }
            is OtpVerificationResult.Expired -> VerificationResult.Failure("OTP has expired")
            is OtpVerificationResult.Invalid ->
                VerificationResult.Failure("Invalid OTP. ${result.remainingAttempts} attempts remaining")
            is OtpVerificationResult.TooManyAttempts ->
                VerificationResult.Failure("Too many attempts. Please request a new OTP")
        }
    }

    suspend fun verifyPhone(userId: String, otp: String): VerificationResult {
        return when (val result = otpService.verifyOtp(userId, OtpPurpose.PHONE_VERIFICATION, otp)) {
            is OtpVerificationResult.Success -> {
                userRepository.updatePhoneVerification(userId, verified = true)
                VerificationResult.Success("Phone verified successfully")
            }
            is OtpVerificationResult.Expired -> VerificationResult.Failure("OTP has expired")
            is OtpVerificationResult.Invalid ->
                VerificationResult.Failure("Invalid OTP. ${result.remainingAttempts} attempts remaining")
            is OtpVerificationResult.TooManyAttempts ->
                VerificationResult.Failure("Too many attempts. Please request a new OTP")
        }
    }

    suspend fun resendOtp(userId: String, purpose: OtpPurpose): ResendResult {
        val user = userRepository.read(userId) ?: return ResendResult.UserNotFound

        if (!otpService.canResendOtp(userId, purpose)) {
            return ResendResult.Cooldown(60) // fixed: was missing `return`, so cooldown was never honored
        }

        val otp = otpService.generateOtp(userId, purpose)

        val channels = when (purpose) {
            OtpPurpose.EMAIL_VERIFICATION -> listOf("email")
            OtpPurpose.PHONE_VERIFICATION -> listOf("sms")
            OtpPurpose.PASSWORD_RESET -> listOf("email")
            OtpPurpose.LOGIN_2FA -> get2FAChannels(user)
            else -> listOf("email")
        }

        notificationAdapter.sendOtpNotification(
            userId = userId,
            otp = otp,
            purpose = purpose.name.lowercase(),
            channels = channels
        )

        return ResendResult.Success
    }

    suspend fun resetPassword(email: String, otp: String, newPassword: String): ResetPasswordResult {
        val user = userRepository.findByEmail(email) ?: return ResetPasswordResult.UserNotFound

        return when (val result = otpService.verifyOtp(user.id, OtpPurpose.PASSWORD_RESET, otp)) {
            is OtpVerificationResult.Success -> {
                userRepository.updatePassword(user.id, PasswordHasher.hash(newPassword))
                tokenService.invalidateAllUserTokens(user.id)
                sessionRepository.deleteAllForUser(user.id)
                ResetPasswordResult.Success
            }
            is OtpVerificationResult.Expired -> ResetPasswordResult.Failure("OTP has expired")
            is OtpVerificationResult.Invalid ->
                ResetPasswordResult.Failure("Invalid OTP. ${result.remainingAttempts} attempts remaining")
            is OtpVerificationResult.TooManyAttempts ->
                ResetPasswordResult.Failure("Too many attempts. Please request a new OTP")
        }
    }

    suspend fun update2FAStatus(userId: String, enabled: Boolean) {
        userRepository.update2FAStatus(userId, enabled)
    }

    suspend fun verify2FA(userId: String, otp: String): VerificationResult {
        return when (val result = otpService.verifyOtp(userId, OtpPurpose.LOGIN_2FA, otp)) {
            is OtpVerificationResult.Success -> VerificationResult.Success("2FA verified successfully")
            is OtpVerificationResult.Expired -> VerificationResult.Failure("OTP has expired")
            is OtpVerificationResult.Invalid ->
                VerificationResult.Failure("Invalid OTP. ${result.remainingAttempts} attempts remaining")
            is OtpVerificationResult.TooManyAttempts ->
                VerificationResult.Failure("Too many attempts. Please request a new OTP")
        }
    }

    private suspend fun generateTokens(
        user: User,
        ipAddress: InetAddress? = null,
        deviceInfo: String? = null
    ): TokenResponse {
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

