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

    val payload: Map<String, @Contextual Any>,

    val processed: Boolean = false,

    @SerialName("processed_at")
    @Contextual val processedAt: OffsetDateTime? = null,

    val attempts: Int = 0,

    @SerialName("error_message")
    val errorMessage: String? = null,

    @Contextual
    @SerialName("created_at")
    val createdAt: OffsetDateTime = OffsetDateTime.now()
)
