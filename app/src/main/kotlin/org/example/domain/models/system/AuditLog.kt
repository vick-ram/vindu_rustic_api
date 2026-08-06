package org.example.domain.models.system

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.example.utils.InetAddressSerializer
import org.example.utils.Ulid
import java.net.InetAddress
import java.time.OffsetDateTime

@Serializable
data class AuditLog(
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

    val changes: Map<String, @Contextual Any>? = null, // old and new

    val metadata: Map<String, @Contextual Any>? = null,

    @Contextual val ipAddress: InetAddress? = null,
    val userAgent: String? = null,

    @Contextual
    @SerialName(value = "created_at")
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
)