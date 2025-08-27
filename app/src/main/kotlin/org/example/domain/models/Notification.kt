package org.example.domain.models

import kotlinx.datetime.LocalDateTime
import org.example.utils.now

data class Notification(
    val id: String,
    val title: String,
    val message: String,
    val recipient: String,
    val channel: NotificationChannel,
    val metadata: Map<String, Any?> = emptyMap(),
    val createdAt: LocalDateTime = LocalDateTime.now()
)

enum class NotificationChannel {
    FCM, EMAIL, DATABASE
}
