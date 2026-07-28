package org.example.domain.models.system

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.example.utils.Ulid
import java.time.OffsetDateTime

@Serializable
data class OutboxEvent(
    val id: String = Ulid.generate(),

    @SerialName("aggregate_type")
    val aggregateType: String,

    @SerialName("aggregate_id")
    val aggregateId: String,

    @SerialName("event_type")
    val eventType: String,

    @Contextual
    val payload: Map<String, Any>,

    val processed: Boolean = false,

    @Contextual
    @SerialName("processed_at")
    val processedAt: OffsetDateTime? = null,

    val attempts: Int = 0,

    @SerialName("error_message")
    val errorMessage: String? = null,

    @Contextual
    @SerialName("created_at")
    val createdAt: OffsetDateTime = OffsetDateTime.now()
)
