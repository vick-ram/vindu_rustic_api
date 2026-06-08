package org.example.domain.models.inventory

import com.google.gson.annotations.SerializedName
import org.example.data.db.config.Ulid
import org.example.domain.validations.GreaterThan
import java.io.Serializable
import java.time.OffsetDateTime

data class Inventory(
    val id: String = Ulid.generate(),

    @SerializedName(value = "variant_id")
    val variantId: String,

    val warehouseId: String,

    @field:GreaterThan(value = 0)
    @SerializedName(value = "availability_quantity")
    val availableQuantity: Int = 0,

    @SerializedName(value = "reserved_quantity")
    val reservedQuantity: Int = 0,

    @field:GreaterThan(value = 0)
    @SerializedName(value = "damaged_quantity")
    val damagedQuantity: Int = 0,

    val lowStockThreshold: Int? = null,

    @SerializedName(value = "updated_at")
    val updatedAt: OffsetDateTime = OffsetDateTime.now()
): Serializable {
    companion object {

        val columns: List<Map<String, Any>> = listOf(
            mapOf("key" to "id", "label" to "ID"),
            mapOf("key" to "variant_id", "label" to "Variant ID"),
            mapOf("key" to "warehouse_id", "label" to "Warehouse ID"),
            mapOf("key" to "available_quantity", "label" to "Available", "sortable" to true),
            mapOf("key" to "reserved_quantity", "label" to "Reserved"),
            mapOf("key" to "damaged_quantity", "label" to "Damaged"),
            mapOf("key" to "updated_at", "label" to "Last Updated", "sortable" to true),
        )

        fun toRows(inventories: List<Inventory>): List<Map<String, Any?>> =
            inventories.map { inventory ->
                mapOf(
                    "id" to inventory.id,
                    "variant_id" to inventory.variantId,
                    "warehouse_id" to inventory.warehouseId,
                    "available_quantity" to inventory.availableQuantity,
                    "reserved_quantity" to inventory.reservedQuantity,
                    "damaged_quantity" to inventory.damagedQuantity,
                    "low_stock_threshold" to inventory.lowStockThreshold,
                    "updated_at" to inventory.updatedAt
                )
            }
    }
}