package org.example.domain.models

import kotlinx.datetime.LocalDateTime
import org.example.utils.now
import org.example.utils.shortUUID

data class AppNotification(
    val id: String = shortUUID(),
    val title: String,
    val message: String,
    val topic: String? = null,
    val channel: NotificationChannel,
    val imageUrl: String? = null,
    val metadata: Map<String, Any>? = emptyMap(),
    val createdAt: LocalDateTime = LocalDateTime.now()
)

enum class NotificationChannel {
    FCM, EMAIL, DATABASE
}

data class DeviceToken(
    val id: String = shortUUID(),
    val userId: String,
    val token: String,
    val platform: Platform = Platform.WEB,
    val createdAt: LocalDateTime = LocalDateTime.now()
)

enum class Platform { ANDROID, IOS, WEB }
