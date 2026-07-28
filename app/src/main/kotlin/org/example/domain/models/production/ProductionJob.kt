package org.example.domain.models.production

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.example.domain.validations.OneOf
import org.example.utils.Ulid
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

    @OneOf("queued", "in_progress", "quality_check", "completed", "on_hold", "cancelled")
    val status: String = "queued",

    @OneOf("low", "normal", "high", "critical")
    val priority: String = "normal",

    val quantity: Int,
    val notes: String? = null,

    @Contextual
    @SerialName(value = "started_at")
    val startedAt: OffsetDateTime? = null,

    @Contextual
    @SerialName(value = "completed_at")
    val completedAt: OffsetDateTime? = null,

    @Contextual
    @SerialName(value = "estimated_completion_at")
    val estimatedCompletionAt: OffsetDateTime? = null,

    @Contextual
    @SerialName(value = "created_at")
    val createdAt: OffsetDateTime = OffsetDateTime.now(),

    @Contextual
    @SerialName(value = "updated_at")
    val updatedAt: OffsetDateTime = OffsetDateTime.now(),
)