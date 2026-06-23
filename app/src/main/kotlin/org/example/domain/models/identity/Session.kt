package org.example.domain.models.identity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.example.data.db.config.Ulid
import org.example.utils.InetAddressSerializer
import org.example.utils.OffsetDateTimeSerializer
import java.net.InetAddress
import java.time.OffsetDateTime

@Serializable
data class Session(
    val id: String = Ulid.generate(),

    @SerialName("user_id")
    val userId: String,

    @SerialName("refresh_token")
    val refreshToken: String, // hashed

    @Serializable(with = InetAddressSerializer::class)
    @SerialName("ip_address")
    val ipAddress: InetAddress? = null,

    @SerialName("user_agent")
    val userAgent: String? = null,

    @Serializable(with = OffsetDateTimeSerializer::class)
    @SerialName("expires_at")
    val expiresAt: OffsetDateTime,

    @Serializable(with = OffsetDateTimeSerializer::class)
    @SerialName("created_at")
    val createdAt: OffsetDateTime = OffsetDateTime.now()
)
