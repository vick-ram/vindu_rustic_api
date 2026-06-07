package org.example.domain.models.identity

import com.google.gson.annotations.SerializedName
import kotlinx.datetime.LocalDateTime
import org.example.utils.now
import java.io.Serializable
import java.time.OffsetDateTime

data class UserRole(
    @SerializedName(value = "user_id")
    val userId: String,

    @SerializedName(value = "role_id")
    val roleId: String,

    @SerializedName(value = "created_at")
    val createdAt: OffsetDateTime = OffsetDateTime.now()
): Serializable