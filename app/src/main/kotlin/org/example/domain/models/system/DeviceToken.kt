package org.example.domain.models.system

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.example.domain.validations.OneOf
import org.example.utils.Ulid
import java.time.OffsetDateTime

@Serializable
data class DeviceToken(
    val id: String = Ulid.generate(),

    @SerialName(value = "user_id")
    val userId: String,

    val token: String,

    @OneOf("android", "ios", "web")
    val platform: String = "web",

    val isActive: Boolean = true,

    @Contextual
    @SerialName(value = "created_at")
    val createdAt: OffsetDateTime = OffsetDateTime.now()
)

@Serializable
data class DispatchResult(
    val notificationId: String,
    val channel: String,
    val success: Boolean,
    val message: String? = null,
    val externalId: String = "",

    @Contextual
    val timestamp: OffsetDateTime = OffsetDateTime.now()
)

@Serializable
data class MultiChannelResult(
    val notificationId: String,
    val results: Map<String, DispatchResult>,
    val successCount: Int,
    val failureCount: Int,
    @Contextual
    val timestamp: OffsetDateTime = OffsetDateTime.now()
)