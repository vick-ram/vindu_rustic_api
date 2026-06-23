package org.example.domain.models.system

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.example.data.db.config.Ulid
import org.example.domain.models.NotificationChannel
import org.example.domain.models.Platform
import org.example.utils.OffsetDateTimeSerializer
import java.time.OffsetDateTime

@Serializable
data class DeviceToken(
    val id: String = Ulid.generate(),

    @SerialName(value = "user_id")
    val userId: String,

    val token: String,
    val platform: Platform = Platform.WEB,

    val isActive: Boolean = true,

    @Serializable(with = OffsetDateTimeSerializer::class)
    @SerialName(value = "created_at")
    val createdAt: OffsetDateTime = OffsetDateTime.now()
)

@Serializable
data class DispatchResult(
    val notificationId: String,
    val channel: NotificationChannel,
    val success: Boolean,
    val message: String? = null,
    val externalId: String = "",

    @Serializable(with = OffsetDateTimeSerializer::class)
    val timestamp: OffsetDateTime = OffsetDateTime.now()
)

@Serializable
data class MultiChannelResult(
    val notificationId: String,
    val results: Map<NotificationChannel, DispatchResult>,
    val successCount: Int,
    val failureCount: Int,
    @Serializable(with = OffsetDateTimeSerializer::class)
    val timestamp: OffsetDateTime = OffsetDateTime.now()
)