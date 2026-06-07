package org.example.data.db.tables

import org.example.data.db.config.CustomTable

object InventoryMovements : CustomTable("inventory_movements") {
    val variantId = reference("variant_id", ProductVariants)
    val warehouseId = reference("warehouse_id", Warehouses)
    val movementType = varchar("movement_type", 50)
    val quantity = integer("quantity")
    val referenceType = varchar("reference_type", 100).nullable()
    val referenceId = long("reference_id").nullable()
    val performedBy = reference("performed_by", Users).nullable()
    val notes = text("notes").nullable()
}