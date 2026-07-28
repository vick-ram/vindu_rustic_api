package org.example.domain.models.identity

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.example.utils.OffsetDateTimeSerializer
import java.time.OffsetDateTime

@Serializable
data class UserRole(
    @SerialName(value = "user_id")
    val userId: String,

    @SerialName(value = "role_id")
    val roleId: String,

    @Contextual
    @SerialName(value = "created_at")
    val createdAt: OffsetDateTime = OffsetDateTime.now()
)