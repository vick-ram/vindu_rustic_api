package org.example.domain.models.sales

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.example.data.db.config.Ulid
import org.example.utils.OffsetDateTimeSerializer
import java.time.OffsetDateTime

@Serializable
data class OrderStatusHistory(
    val id: String = Ulid.generate(),

    @SerialName(value = "order_id")
    val orderId: String,

    @SerialName(value = "old_status")
    val oldStatus: String? = null,

    @SerialName("new_status")
    val newStatus: String,

    @SerialName("changed_by")
    val changedBy: String? = null,

    val comment: String? = null,

    @Serializable(with = OffsetDateTimeSerializer::class)
    @SerialName("created_at")
    val createdAt: OffsetDateTime = OffsetDateTime.now()
)
