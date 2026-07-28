package org.example.domain.models.system

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.example.domain.validations.OneOf
import org.example.utils.Ulid
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

    @Contextual
    val readAt: OffsetDateTime? = null,

    @OneOf("database", "fcm", "sms", "email")
    val channel: String = "database",

    @Contextual
    @SerialName(value = "created_at")
    val createdAt: OffsetDateTime = OffsetDateTime.now(),

    @Contextual
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