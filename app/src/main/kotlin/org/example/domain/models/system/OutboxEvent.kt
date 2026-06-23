package org.example.domain.models.system

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.example.data.db.config.Ulid
import org.example.utils.MapStringAnySerializer
import org.example.utils.OffsetDateTimeSerializer
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

    @Serializable(with = MapStringAnySerializer::class)
    val payload: Map<String, Any>,

    val processed: Boolean = false,

    @Serializable(with = OffsetDateTimeSerializer::class)
    @SerialName("processed_at")
    val processedAt: OffsetDateTime? = null,

    val attempts: Int = 0,

    @SerialName("error_message")
    val errorMessage: String? = null,

    @Serializable(with = OffsetDateTimeSerializer::class)
    @SerialName("created_at")
    val createdAt: OffsetDateTime = OffsetDateTime.now()
)
