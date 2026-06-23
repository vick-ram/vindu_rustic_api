package org.example.domain.models.production

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.example.data.db.config.Ulid
import org.example.utils.OffsetDateTimeSerializer
import java.time.OffsetDateTime

@Serializable
data class ProductionJob(
    val id: String = Ulid.generate(),

    @SerialName(value = "order_item_id")
    val orderItemId: String,

    @SerialName(value = "product_id")
    val productId: String,

    @SerialName("assigned_to")
    val assignedTo: String? = null,

    val status: String = "QUEUED",

    val priority: String = "NORMAL",
    val quantity: Int,

    val notes: String? = null,

    @Serializable(with = OffsetDateTimeSerializer::class)
    @SerialName(value = "started_at")
    val startedAt: OffsetDateTime? = null,

    @Serializable(with = OffsetDateTimeSerializer::class)
    @SerialName(value = "completed_at")
    val completedAt: OffsetDateTime? = null,

    @Serializable(with = OffsetDateTimeSerializer::class)
    @SerialName(value = "estimated_completion_at")
    val estimatedCompletionAt: OffsetDateTime? = null,

    @Serializable(with = OffsetDateTimeSerializer::class)
    @SerialName(value = "created_at")
    val createdAt: OffsetDateTime = OffsetDateTime.now(),

    @Serializable(with = OffsetDateTimeSerializer::class)
    @SerialName(value = "updated_at")
    val updatedAt: OffsetDateTime = OffsetDateTime.now(),
)