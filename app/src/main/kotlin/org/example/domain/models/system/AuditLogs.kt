package org.example.domain.models.system

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.example.data.db.config.Ulid
import org.example.utils.InetAddressSerializer
import org.example.utils.MapStringAnySerializer
import org.example.utils.OffsetDateTimeSerializer
import java.net.InetAddress
import java.time.OffsetDateTime

@Serializable
data class AuditLogs(
    val id: String = Ulid.generate(),

    @SerialName(value = "actor_id")
    val actorId: String? = null,

    @SerialName(value = "actor_type")
    val actorType: String,

    val action: String,

    @SerialName(value = "entity_type")
    val entityType: String,

    @SerialName(value = "entity_id")
    val entityId: String,

    @Serializable(with = MapStringAnySerializer::class)
    val changes: Map<String, Any>? = null, // old and new

    @Serializable(with = MapStringAnySerializer::class)
    val metadata: Map<String, Any>? = null,

    @Serializable(with = InetAddressSerializer::class)
    val ipAddress: InetAddress? = null,
    val userAgent: String? = null,

    @Serializable(with = OffsetDateTimeSerializer::class)
    @SerialName(value = "created_at")
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
)