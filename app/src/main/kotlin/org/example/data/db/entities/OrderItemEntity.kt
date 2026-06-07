package org.example.data.db.entities

import org.example.data.db.config.CustomEntity
import org.example.data.db.config.CustomEntityClass
import org.example.data.db.tables.OrderItems
import org.jetbrains.exposed.v1.core.dao.id.EntityID

class OrderItemEntity(id: EntityID<String>) : CustomEntity(id, OrderItems) {
    companion object : CustomEntityClass<OrderItemEntity>(OrderItems)

    var orderId by OrderItems.orderId
    var productId by OrderItems.productId
    var variantId by OrderItems.variantId
    var warehouseId by OrderItems.warehouseId
    var quantity by OrderItems.quantity
    var unitPrice by OrderItems.unitPrice
    var totalPrice by OrderItems.totalPrice
    var customizationSnapshot by OrderItems.customizationSnapshot
    var productSnapshot by OrderItems.productSnapshot
}