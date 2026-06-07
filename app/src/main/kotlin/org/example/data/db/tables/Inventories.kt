package org.example.data.db.tables

import org.example.data.db.config.CustomTable

object Inventories : CustomTable("inventory") {
    val variantId = reference("variant_id", ProductVariants)
    val warehouseId = reference("warehouse_id", Warehouses)
    val availableQuantity = integer("available_quantity").default(0)
    val reservedQuantity = integer("reserved_quantity").default(0)
    val damagedQuantity = integer("damaged_quantity").default(0)
    val lowStockThreshold = integer("low_stock_threshold").nullable()

    init {
        uniqueIndex(variantId, warehouseId)
    }
}