package org.example.utils.notifications

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.*
import org.example.config.AppConfig
import org.example.domain.models.system.Notification
import org.example.domain.repo.NotificationRepository
import org.example.services.DeviceTokenService
import java.io.File

class FcmNotificationService(private val config: AppConfig, private val deviceTokenService: DeviceTokenService) :
    NotificationRepository {
    private val firebaseMessaging = FirebaseMessaging.getInstance()

    init {
        initializeFirebase()
    }

    override suspend fun send(notification: Notification) {
        when {
            notification.topic != null -> {
                sendToTopic(
                    title = notification.title,
                    body = notification.body,
                    topic = notification.topic,
                    data = notification.metadata?.mapValues { it.value.toString() } ?: emptyMap(),
                    imageUrl = notification.imageUrl
                )
            }

            else -> {
                val userId = notification.metadata?.get("userId").toString()
                val deviceTokens =
                    deviceTokenService.getUserDeviceTokens(userId)
                val tokens = deviceTokens.map { it.token }
                if (tokens.size > 1) {
                    sendToMultiple(
                        tokens = tokens,
                        title = notification.title,
                        body = notification.body,
                        data = notification.metadata?.mapValues { it.value.toString() } ?: emptyMap(),
                        imageUrl = notification.imageUrl
                    )
                } else if (tokens.isNotEmpty()) {
                    sendToDevice(
                        token = tokens.first(),
                        title = notification.title,
                        body = notification.body,
                        data = notification.metadata?.mapValues { it.value.toString() } ?: emptyMap(),
                        imageUrl = notification.imageUrl
                    )
                }
            }
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

    fun sendToDevice(token: String, title: String, body: String, data: Map<String, String>, imageUrl: String?): String {
        return firebaseMessaging.send(
            buildMessage(
                token = token,
                title = title,
                body = body,
                data = data,
                imageUrl = imageUrl
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
                imageUrl = imageUrl
            )
        )
    }

    fun sendToMultiple(
        tokens: List<String>,
        title: String,
        body: String,
        data: Map<String, String>,
        imageUrl: String?
    ): BatchResponse {
        return firebaseMessaging.sendEachForMulticast(
            buildMulticastMessage(
                tokens = tokens,
                title = title,
                body = body,
                data = data,
                imageUrl = imageUrl
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
        data: Map<String, String> = emptyMap(),
        imageUrl: String? = null
    ): Message {
        val builder = Message.builder()
            .setNotification(
                Notification.builder()
                    .setTitle(title)
                    .setBody(body)
                    .setImage(imageUrl)
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
                Notification.builder()
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