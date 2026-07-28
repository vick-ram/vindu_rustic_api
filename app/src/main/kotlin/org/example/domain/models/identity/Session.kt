package org.example.domain.models.identity

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.example.utils.InetAddressSerializer
import org.example.utils.Ulid
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

    @Contextual
    @SerialName("expires_at")
    val expiresAt: OffsetDateTime,

    @Contextual
    @SerialName("created_at")
    val createdAt: OffsetDateTime = OffsetDateTime.now()
)
