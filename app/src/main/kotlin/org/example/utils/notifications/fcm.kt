package org.example.utils.notifications

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.AndroidConfig
import com.google.firebase.messaging.ApnsConfig
import com.google.firebase.messaging.Aps
import com.google.firebase.messaging.BatchResponse
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingException
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.MessagingErrorCode
import com.google.firebase.messaging.MulticastMessage
import com.google.firebase.messaging.TopicManagementResponse
import com.google.firebase.messaging.Notification as FCMNotification
import org.example.config.AppConfig
import org.example.data.repo.DeviceTokenRepository
import org.example.domain.models.NotificationChannel
import org.example.domain.models.system.DispatchResult
import org.example.domain.models.system.Notification
import org.koin.core.annotation.Single
import org.slf4j.LoggerFactory
import java.io.File

@Single
class FcmNotificationService(private val config: AppConfig) {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val firebaseMessaging = FirebaseMessaging.getInstance()

    init {
        initializeFirebase()
    }

    suspend fun send(notificationId: String, tokens: List<String>, title: String, body: String, data: Map<String, String>, deviceTokenRepository: DeviceTokenRepository): DispatchResult {

        return try {
            val result = if (tokens.size == 1) {
                val messageId = sendToDevice(
                    token = tokens[0],
                    title = title,
                    body = body,
                    data = data
                )

                DispatchResult(
                    notificationId = notificationId,
                    channel = NotificationChannel.FCM,
                    success = true,
                    message = "Sent to single device",
                    externalId = messageId
                )
            } else {
                val batchResponse = sendToMultiple(
                    tokens = tokens,
                    title = title,
                    body = body,
                    data = data
                )

                val failureCount = batchResponse.failureCount

                // Deactivate invalid tokens
                if (failureCount > 0) {
                    batchResponse.responses.forEachIndexed { index, response ->
                        if (!response.isSuccessful) {
                            val failedToken = tokens[index]
                            logger.warn("Failed to send to token: $failedToken")
                            // Deactivate invalid tokens
                            if (response.exception is FirebaseMessagingException) {
                                val fcmError = response.exception as FirebaseMessagingException
                                if (fcmError.messagingErrorCode == MessagingErrorCode.UNREGISTERED) {
                                    deviceTokenRepository.deactivateToken(failedToken)
                                }
                            }
                        }
                    }
                }

                DispatchResult(
                    notificationId = notificationId,
                    channel = NotificationChannel.FCM,
                    success = failureCount < tokens.size,
                    message = "Sent to ${tokens.size - failureCount}/${tokens.size} devices",
                    externalId = batchResponse.responses.firstOrNull { it.isSuccessful }?.messageId ?: ""
                )
            }
            result
        } catch (e: Exception) {
            logger.error("Failed to send notification $notificationId", e)
            throw e
        }
    }

    private fun buildNotificationData(notification: Notification): Map<String, String> {
        return mutableMapOf(
            "notification_id" to notification.id,
            "type" to notification.type,
            "action_url" to (notification.actionUrl ?: ""),
            "reference_type" to (notification.referenceType ?: ""),
            "reference_id" to (notification.referenceId ?: "")
        ).apply {
            putAll(notification.metadata.filterKeys { it != "imageUrl" })
        }
    }

    private fun initializeFirebase() {
        config.fcm.credentialPath?.let { path ->
            val serviceAccount = File(path).inputStream()
            val options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .build()

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options)
            }
        } ?: run {
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp()
            }
        }
    }

    fun sendToDevice(token: String, title: String, body: String, data: Map<String, String>): String {
        return firebaseMessaging.send(
            buildMessage(
                token = token,
                title = title,
                body = body,
                data = data,
            )
        )
    }

    fun sendToTopic(title: String, body: String, topic: String, data: Map<String, String>, imageUrl: String?): String {
        return firebaseMessaging.send(
            buildMessage(
                title = title,
                body = body,
                topic = topic,
                data = data,
            )
        )
    }

    fun sendToMultiple(
        tokens: List<String>,
        title: String,
        body: String,
        data: Map<String, String>
    ): BatchResponse {
        return firebaseMessaging.sendEachForMulticast(
            buildMulticastMessage(
                tokens = tokens,
                title = title,
                body = body,
                data = data,
            )
        )
    }

    fun subscribeToTopic(
        tokens: List<String>,
        topic: String
    ): TopicManagementResponse {
        return firebaseMessaging.subscribeToTopic(tokens, topic)
    }

    fun unsubscribeFromTopic(
        tokens: List<String>,
        topic: String
    ): TopicManagementResponse {
        return firebaseMessaging.unsubscribeFromTopic(tokens, topic)
    }

    private fun buildMessage(
        token: String? = null,
        topic: String? = null,
        condition: String? = null,
        title: String,
        body: String,
        data: Map<String, String> = emptyMap()
    ): Message {
        val builder = Message.builder()
            .setNotification(
                FCMNotification.builder()
                    .setTitle(title)
                    .setBody(body)
                    .build()
            )
            .putAllData(data)
            .setAndroidConfig(
                AndroidConfig.builder()
                    .setPriority(AndroidConfig.Priority.HIGH)
                    .build()
            )
            .setApnsConfig(
                ApnsConfig.builder()
                    .setAps(Aps.builder().setSound("default").build())
                    .build()
            )
        token?.let { builder.setToken(it) }
        topic?.let { builder.setTopic(it) }
        condition?.let { builder.setCondition(it) }

        return builder.build()
    }

    private fun buildMulticastMessage(
        tokens: List<String>,
        title: String,
        body: String,
        data: Map<String, String> = emptyMap(),
        imageUrl: String? = null
    ): MulticastMessage {
        return MulticastMessage.builder()
            .setNotification(
                FCMNotification.builder()
                    .setTitle(title)
                    .setBody(body)
                    .setImage(imageUrl)
                    .build()
            )
            .addAllTokens(tokens)
            .putAllData(data)
            .setAndroidConfig(
                AndroidConfig.builder()
                    .setPriority(AndroidConfig.Priority.HIGH)
                    .build()
            )
            .setApnsConfig(
                ApnsConfig.builder()
                    .setAps(Aps.builder().setSound("default").build())
                    .build()
            )
            .build()
    }
}