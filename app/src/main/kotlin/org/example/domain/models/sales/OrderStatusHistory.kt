package org.example.domain.models.sales

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.example.domain.validations.OneOf
import org.example.utils.Ulid
import java.time.OffsetDateTime

@Serializable
data class OrderStatusHistory(
    val id: String = Ulid.generate(),

    @SerialName(value = "order_id")
    val orderId: String,

    @SerialName(value = "old_status")
    val oldStatus: String? = null,

    @OneOf("pending", "confirmed", "processing", "completed", "cancelled", "refunded")
    @SerialName("new_status")
    val newStatus: String,

    @SerialName("changed_by")
    val changedBy: String? = null,

    val comment: String? = null,

    @Contextual
    @SerialName("created_at")
    val createdAt: OffsetDateTime = OffsetDateTime.now()
)
