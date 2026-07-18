package org.example.domain.repo

import io.celery.CeleryApp
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import org.example.di.Inject
import org.example.di.Injectable
import org.example.domain.models.NotificationChannel
import org.example.domain.models.system.DispatchResult
import org.example.domain.models.system.Notification
import org.slf4j.LoggerFactory
import java.time.OffsetDateTime

@Injectable
class NotificationAdapter @Inject constructor(
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