package org.example.domain.models.inventory

import com.google.gson.annotations.SerializedName
import kotlinx.datetime.LocalDateTime
import org.example.utils.now
import org.example.utils.shortUUID
import java.io.Serializable
import java.time.OffsetDateTime

data class Inventory(
    val id: String = shortUUID(),

    @SerializedName(value = "variant_id")
    val variantId: String,

    val warehouseId: String,

    @SerializedName(value = "availability_quantity")
    val availableQuantity: Int = 0,

    @SerializedName(value = "reserved_quantity")
    val reservedQuantity: Int = 0,

    @SerializedName(value = "damaged_quantity")
    val damagedQuantity: Int = 0,

    val lowStockThreshold: Int? = null,

    @SerializedName(value = "updated_at")
    val updatedAt: OffsetDateTime = OffsetDateTime.now()
): Serializable