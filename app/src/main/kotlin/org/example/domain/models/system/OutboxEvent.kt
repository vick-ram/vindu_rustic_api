package org.example.domain.models.system

import com.google.gson.annotations.SerializedName
import kotlinx.datetime.LocalDateTime
import org.example.utils.now
import org.example.utils.shortUUID
import java.io.Serializable
import java.time.OffsetDateTime

data class OutboxEvent(
    val id: String = shortUUID(),

    @SerializedName("aggregate_type")
    val aggregateType: String,

    @SerializedName("aggregate_id")
    val aggregateId: String,

    @SerializedName("event_type")
    val eventType: String,

    val payload: Map<String, Any>,
    val processed: Boolean = false,

    @SerializedName("processed_at")
    val processedAt: OffsetDateTime? = null,
    val attempts: Int = 0,

    @SerializedName("error_message")
    val errorMessage: String? = null,

    @SerializedName("created_at")
    val createdAt: OffsetDateTime = OffsetDateTime.now()
): Serializable
