package org.example.data.db.tables

import org.example.data.db.config.CustomTable
import org.jetbrains.exposed.v1.datetime.datetime
import org.jetbrains.exposed.v1.datetime.timestampWithTimeZone

object InventoryReservations : CustomTable("inventory_reservations") {
    val variantId = reference("variant_id", ProductVariants)
    val warehouseId = reference("warehouse_id", Warehouses)
    val cartId = reference("cart_id", ShoppingCarts).nullable()
    val orderId = reference("order_id", Orders).nullable()
    val quantity = integer("quantity")
    val status = varchar("status", 50).default("ACTIVE")
    val expiresAt = timestampWithTimeZone("expires_at")
}