package org.example.utils.notifications

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.*
import org.example.domain.models.AppNotification
import org.example.domain.repo.NotificationRepository
import org.example.services.DeviceTokenService
import org.example.utils.FcmConfig
import java.io.File

class FcmNotificationService(private val config: FcmConfig, private val deviceTokenService: DeviceTokenService) :
    NotificationRepository {
    private val firebaseMessaging = FirebaseMessaging.getInstance()

        init {
            initializeFirebase()
        }
    override suspend fun send(appNotification: AppNotification) {
        when {
            appNotification.topic != null -> {
                sendToTopic(
                    title = appNotification.title,
                    body = appNotification.message,
                    topic = appNotification.topic,
                    data = appNotification.metadata?.mapValues { it.value.toString() } ?: emptyMap(),
                    imageUrl = appNotification.imageUrl
                )
            }

            else -> {
                val userId = appNotification.metadata?.get("userId").toString()
                val deviceTokens =
                    deviceTokenService.getUserDeviceTokens(userId)
                val tokens = deviceTokens.map { it.token }
                if (tokens.size > 1) {
                    sendToMultiple(
                        tokens = tokens,
                        title = appNotification.title,
                        body = appNotification.message,
                        data = appNotification.metadata?.mapValues { it.value.toString() } ?: emptyMap(),
                        imageUrl = appNotification.imageUrl
                    )
                } else if (tokens.isNotEmpty()) {
                    sendToDevice(
                        token = tokens.first(),
                        title = appNotification.title,
                        body = appNotification.message,
                        data = appNotification.metadata?.mapValues { it.value.toString() } ?: emptyMap(),
                        imageUrl = appNotification.imageUrl
                    )
                }
            }
        }
    }

    private fun initializeFirebase() {
        config.credentialPath?.let { path ->
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