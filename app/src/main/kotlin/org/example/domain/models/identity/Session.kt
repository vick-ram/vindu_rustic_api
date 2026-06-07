package org.example.domain.models.identity

import com.google.gson.annotations.SerializedName
import kotlinx.datetime.LocalDateTime
import org.example.utils.now
import org.example.utils.shortUUID
import java.io.Serializable
import java.net.InetAddress
import java.time.OffsetDateTime

data class Session(
    val id: String = shortUUID(),

    @SerializedName("user_id")
    val userId: String,

    @SerializedName("refresh_token")
    val refreshToken: String, // hashed

    @SerializedName("ip_address")
    val ipAddress: InetAddress? = null,

    @SerializedName("user_agent")
    val userAgent: String? = null,

    @SerializedName("expires_at")
    val expiresAt: OffsetDateTime,

    @SerializedName("created_at")
    val createdAt: OffsetDateTime = OffsetDateTime.now()
): Serializable
