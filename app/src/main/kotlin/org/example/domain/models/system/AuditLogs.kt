package org.example.domain.models.system

import com.google.gson.annotations.SerializedName
import org.example.data.db.config.Ulid
import java.io.Serializable
import java.net.InetAddress
import java.time.OffsetDateTime

data class AuditLogs(
    val id: String = Ulid.generate(),

    @SerializedName(value = "actor_id")
    val actorId: String? = null,

    @SerializedName(value = "actor_type")
    val actorType: String,

    val action: String,

    @SerializedName(value = "entity_type")
    val entityType: String,

    @SerializedName(value = "entity_id")
    val entityId: String,

    val changes: Map<String, Any>? = null, // old and new

    val metadata: Map<String, Any>? = null,

    val ipAddress: InetAddress? = null,
    val userAgent: String? = null,

    @SerializedName(value = "created_at")
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
): Serializable