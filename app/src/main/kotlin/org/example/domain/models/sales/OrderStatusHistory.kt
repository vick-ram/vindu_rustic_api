package org.example.domain.models.sales

import com.google.gson.annotations.SerializedName
import kotlinx.datetime.LocalDateTime
import org.example.utils.now
import org.example.utils.shortUUID
import java.io.Serializable
import java.time.OffsetDateTime

data class OrderStatusHistory(
    val id: String = shortUUID(),

    @SerializedName(value = "order_id")
    val orderId: String,

    @SerializedName(value = "old_status")
    val oldStatus: String? = null,

    @SerializedName("new_status")
    val newStatus: String,

    @SerializedName("changed_by")
    val changedBy: String? = null,

    val comment: String? = null,

    @SerializedName("created_at")
    val createdAt: OffsetDateTime = OffsetDateTime.now()
): Serializable
