package org.example.utils.notifications

import org.example.config.AppConfig
import org.example.domain.models.NotificationChannel
import org.example.domain.models.system.DispatchResult
import org.example.domain.models.system.Notification
import org.example.domain.repo.NotificationRepository
import org.slf4j.LoggerFactory
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sns.SnsClient
import software.amazon.awssdk.services.sns.model.MessageAttributeValue
import software.amazon.awssdk.services.sns.model.PublishRequest

class SMSService(private val config: AppConfig) : NotificationRepository {
    private val logger = LoggerFactory.getLogger(javaClass)

    private val snsClient by lazy {
        val credentials = AwsBasicCredentials.create(config.sms.accessKey, config.sms.secretKey)
        SnsClient.builder()
            .region(Region.of(config.sms.region))
            .credentialsProvider(StaticCredentialsProvider.create(credentials))
            .build()
    }

    override suspend fun send(notification: Notification, email: String?, phoneNumber: String?): DispatchResult {
        return try {
            val attributes = mapOf(
                "AWS.SNS.SMS.SenderID" to MessageAttributeValue.builder()
                    .dataType("String")
                    .stringValue("NOTIFY")
                    .build(),
                "AWS.SNS.SMS.SMSType" to MessageAttributeValue.builder()
                    .dataType("String")
                    .stringValue("Transactional")
                    .build()
            )

            val request = PublishRequest.builder()
                .message(notification.body)
                .phoneNumber(phoneNumber)
                .messageAttributes(attributes)
                .build()

            snsClient?.publish(request)
            logger.info("SMS sent for notification ${notification.id} to $phoneNumber")

            DispatchResult(
                notificationId = notification.id,
                channel = NotificationChannel.SMS,
                success = true,
                message = "SMS sent to $phoneNumber",
            )
        } catch (e: Exception) {
            logger.error("Failed to send SMS for notification ${notification.id}", e)
            throw e
        }
    }

    override suspend fun markAsRead(notificationId: String, userId: String) {
        logger.debug("SMS does not support markAsRead")
    }

    override suspend fun getUnreadCount(userId: String): Long = 0
}