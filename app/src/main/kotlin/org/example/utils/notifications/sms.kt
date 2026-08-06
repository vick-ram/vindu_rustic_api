package org.example.utils.notifications

import org.example.config.AppConfig
import org.example.di.Component
import org.example.di.Inject
import org.example.domain.models.system.DispatchResult
import org.slf4j.LoggerFactory
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sns.SnsClient
import software.amazon.awssdk.services.sns.model.MessageAttributeValue
import software.amazon.awssdk.services.sns.model.PublishRequest

@Component
class SMSService @Inject constructor(private val config: AppConfig) {
    private val logger = LoggerFactory.getLogger(javaClass)

    private val snsClient by lazy {
        val credentials = AwsBasicCredentials.create(config.sms.accessKey, config.sms.secretKey)
        SnsClient.builder()
            .region(Region.of(config.sms.region))
            .credentialsProvider(StaticCredentialsProvider.create(credentials))
            .build()
    }

    suspend fun send(notificationId: String ,phoneNumber: String?, message: String): DispatchResult {
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
                .message(message)
                .phoneNumber(phoneNumber)
                .messageAttributes(attributes)
                .build()

            snsClient?.publish(request)
            logger.info("SMS sent for notification $notificationId to $phoneNumber")

            DispatchResult(
                notificationId = notificationId,
                channel = "sms",
                success = true,
                message = "SMS sent to $phoneNumber",
            )
        } catch (e: Exception) {
            logger.error("Failed to send SMS for notification $notificationId", e)
            throw e
        }
    }
}