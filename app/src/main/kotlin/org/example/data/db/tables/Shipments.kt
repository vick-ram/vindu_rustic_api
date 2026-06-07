package org.example.data.db.tables

import org.example.data.db.config.CustomTable
import org.jetbrains.exposed.v1.datetime.datetime
import org.jetbrains.exposed.v1.datetime.timestampWithTimeZone

object Shipments : CustomTable("shipments") {
    val orderId = reference("order_id", Orders)
    val warehouseId = reference("warehouse_id", Warehouses).nullable()
    val courier = varchar("courier", 255).nullable()
    val serviceLevel = varchar("service_level", 100).nullable()
    val trackingNumber = varchar("tracking_number", 255).nullable()
    val trackingUrl = text("tracking_url").nullable()
    val shippingLabelUrl = text("shipping_label_url").nullable()
    val cost = decimal("cost", 12, 2).nullable()
    val estimatedDeliveryAt = timestampWithTimeZone("estimated_delivery_at").nullable()
}