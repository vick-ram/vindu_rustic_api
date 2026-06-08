package org.example.domain.models.production

import com.google.gson.annotations.SerializedName
import kotlinx.datetime.LocalDateTime
import org.example.data.db.config.Ulid
import java.io.Serializable
import java.time.OffsetDateTime

data class ProductionJob(
    val id: String = Ulid.generate(),

    @SerializedName(value = "order_item_id")
    val orderItemId: String,

    @SerializedName(value = "product_id")
    val productId: String,

    @SerializedName("assigned_to")
    val assignedTo: String? = null,

    val status: String = "QUEUED",

    val priority: String = "NORMAL",
    val quantity: Int,

    val notes: String? = null,

    @SerializedName(value = "started_at")
    val startedAt: OffsetDateTime? = null,

    @SerializedName(value = "completed_at")
    val completedAt: OffsetDateTime? = null,

    @SerializedName(value = "estimated_completion_at")
    val estimatedCompletionAt: OffsetDateTime? = null,

    @SerializedName(value = "created_at")
    val createdAt: OffsetDateTime = OffsetDateTime.now(),

    @SerializedName(value = "updated_at")
    val updatedAt: OffsetDateTime = OffsetDateTime.now(),
): Serializable