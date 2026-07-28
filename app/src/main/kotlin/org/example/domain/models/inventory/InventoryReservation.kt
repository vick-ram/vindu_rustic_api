package org.example.domain.models.inventory

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.example.domain.validations.OneOf
import org.example.utils.Ulid
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

    @OneOf("active", "completed", "expired", "cancelled")
    val status: String = "active",

    @Contextual
    @SerialName("expires_at")
    val expiresAt: OffsetDateTime,

    @Contextual
    @SerialName("created_at")
    val createdAt:OffsetDateTime = OffsetDateTime.now()
)
