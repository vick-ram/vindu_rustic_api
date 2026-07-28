package org.example.domain.models.inventory

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.example.domain.validations.GreaterThan
import org.example.utils.Ulid
import java.time.OffsetDateTime

@Serializable
data class Inventory(
    val id: String = Ulid.generate(),

    @SerialName(value = "variant_id")
    val variantId: String,

    val warehouseId: String,

    @GreaterThan(value = 0)
    @SerialName(value = "availability_quantity")
    val availableQuantity: Int = 0,

    @SerialName(value = "reserved_quantity")
    val reservedQuantity: Int = 0,

    @GreaterThan(value = 0)
    @SerialName(value = "damaged_quantity")
    val damagedQuantity: Int = 0,

    val lowStockThreshold: Int? = null,

    @Contextual
    @SerialName(value = "updated_at")
    val updatedAt: OffsetDateTime = OffsetDateTime.now()
) {
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