package org.example.data.db.tables

import kotlinx.serialization.json.Json
import org.example.data.db.config.CustomTable
import org.jetbrains.exposed.v1.json.jsonb

object OrderItems : CustomTable("order_items",) {
    val orderId = reference("order_id", Orders)
    val productId = reference("product_id", Products)
    val variantId = reference("variant_id", ProductVariants)
    val warehouseId = reference("warehouse_id", Warehouses).nullable()
    val quantity = integer("quantity").check { it greater 0 }
    val unitPrice = decimal("unit_price", 12, 2)
    val totalPrice = decimal("total_price", 12, 2)
    val customizationSnapshot = jsonb<Map<String, Any>>("customization_snapshot", Json).nullable()
    val productSnapshot = jsonb<Map<String, Any>>("product_snapshot", Json)
}