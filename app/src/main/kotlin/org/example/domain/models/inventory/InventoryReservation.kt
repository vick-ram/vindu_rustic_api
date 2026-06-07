package org.example.domain.models.inventory

import com.google.gson.annotations.SerializedName
import kotlinx.datetime.LocalDateTime
import org.example.utils.now
import org.example.utils.shortUUID
import java.io.Serializable
import java.time.OffsetDateTime

data class InventoryReservation(
    val id: String = shortUUID(),

    @SerializedName("variant_id")
    val variantId: String,

    @SerializedName("warehouse_id")
    val warehouseId: String,

    @SerializedName("cart_id")
    val cartId: String? = null,

    @SerializedName("order_id")
    val orderId: String? = null,

    val quantity: Int,
    val status: String = "active",

    @SerializedName("expires_at")
    val expiresAt: OffsetDateTime,

    @SerializedName("created_at")
    val createdAt:OffsetDateTime = OffsetDateTime.now()
): Serializable
