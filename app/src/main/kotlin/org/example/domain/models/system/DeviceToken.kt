package org.example.domain.models.system

import com.google.gson.annotations.SerializedName
import org.example.data.db.config.Ulid
import org.example.domain.models.Platform
import java.time.OffsetDateTime

data class DeviceToken(
    val id: String = Ulid.generate(),

    @SerializedName(value = "user_id")
    val userId: String,

    val token: String,
    val platform: Platform = Platform.WEB,

    @SerializedName(value = "created_at")
    val createdAt: OffsetDateTime = OffsetDateTime.now()
)