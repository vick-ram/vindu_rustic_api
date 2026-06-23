package org.example.domain.models.system

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.example.data.db.config.Ulid
import org.example.domain.models.NotificationChannel
import org.example.utils.OffsetDateTimeSerializer
import java.time.OffsetDateTime

@Serializable
data class Notification(
    val id: String = Ulid.generate(),

    @SerialName("user_id")
    val userId: String,

    val type: String,
    val title: String,
    val body: String? = null,

    @SerialName("action_url")
    val actionUrl: String? = null,

    @SerialName("reference_type")
    val referenceType: String? = null,

    @SerialName("reference_id")
    val referenceId: String? = null,

    val metadata: Map<String, String> = emptyMap(),

    val isRead: Boolean = false,

    @Serializable(with = OffsetDateTimeSerializer::class)
    val readAt: OffsetDateTime? = null,

    val channel: NotificationChannel = NotificationChannel.DATABASE,

    @Serializable(with = OffsetDateTimeSerializer::class)
    @SerialName(value = "created_at")
    val createdAt: OffsetDateTime = OffsetDateTime.now(),

    @Serializable(with = OffsetDateTimeSerializer::class)
    @SerialName("updated_at")
    val updatedAt: OffsetDateTime = OffsetDateTime.now()
)

@Serializable
enum class NotificationPriority {
    LOW,
    NORMAL,
    HIGH,
    CRITICAL
}