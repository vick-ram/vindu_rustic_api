package org.example.utils.notifications

import org.example.config.AppConfig
import org.example.domain.models.system.Notification
import org.example.domain.repo.NotificationRepository
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sns.SnsClient
import software.amazon.awssdk.services.sns.model.MessageAttributeValue
import software.amazon.awssdk.services.sns.model.PublishRequest

class SMSService(private val config: AppConfig) : NotificationRepository {
    private val snsClient by lazy {
        val credentials = AwsBasicCredentials.create(config.sms.accessKey, config.sms.secretKey)
        SnsClient.builder()
            .region(Region.of(config.sms.region))
            .credentialsProvider(StaticCredentialsProvider.create(credentials))
            .build()
    }

    override suspend fun send(notification: Notification) {
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
            .phoneNumber(notification.metadata?.get("phoneNumber").toString())
            .messageAttributes(attributes)
            .build()

        snsClient?.publish(request)
    }
}