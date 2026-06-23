package org.example.domain.models.inventory

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.example.data.db.config.Ulid
import org.example.utils.OffsetDateTimeSerializer
import java.time.OffsetDateTime

@Serializable
data class InventoryReservation(
    val id: String = Ulid.generate(),

    @SerialName("variant_id")
    val variantId: String,

    @SerialName("warehouse_id")
    val warehouseId: String,

    @SerialName("cart_id")
    val cartId: String? = null,

    @SerialName("order_id")
    val orderId: String? = null,

    val quantity: Int,
    val status: String = "active",

    @Serializable(with = OffsetDateTimeSerializer::class)
    @SerialName("expires_at")
    val expiresAt: OffsetDateTime,

    @Serializable(with = OffsetDateTimeSerializer::class)
    @SerialName("created_at")
    val createdAt:OffsetDateTime = OffsetDateTime.now()
)
