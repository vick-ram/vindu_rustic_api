package org.example.domain.models.system

import com.google.gson.annotations.SerializedName
import kotlinx.datetime.LocalDateTime
import org.example.domain.models.Platform
import org.example.utils.now
import org.example.utils.shortUUID
import java.time.OffsetDateTime

data class DeviceToken(
    val id: String = shortUUID(),

    @SerializedName(value = "user_id")
    val userId: String,

    val token: String,
    val platform: Platform = Platform.WEB,

    @SerializedName(value = "created_at")
    val createdAt: OffsetDateTime = OffsetDateTime.now()
)