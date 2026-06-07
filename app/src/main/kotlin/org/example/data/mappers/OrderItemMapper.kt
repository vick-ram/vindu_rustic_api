package org.example.data.mappers

import org.example.data.db.entities.OrderItemEntity
import org.example.data.db.tables.Orders
import org.example.data.db.tables.ProductVariants
import org.example.data.db.tables.Products
import org.example.data.db.tables.Warehouses
import org.example.domain.models.sales.OrderItem
import org.example.domain.repo.EntityMapper
import org.jetbrains.exposed.v1.core.dao.id.EntityID

object OrderItemMapper : EntityMapper<OrderItemEntity, OrderItem, String> {
    override fun toModel(entity: OrderItemEntity): OrderItem {
        return OrderItem(
            id = entity.id.value,
            orderId = entity.orderId.value,
            productId = entity.productId.value,
            variantId = entity.variantId.value,
            warehouseId = entity.warehouseId?.value,
            quantity = entity.quantity,
            unitPrice = entity.unitPrice,
            totalPrice = entity.totalPrice,
            customizationSnapshot = entity.customizationSnapshot,
            productSnapshot = entity.productSnapshot,
            createdAt = entity.createdAt,
        )
    }

    override fun toEntity(model: OrderItem, entity: OrderItemEntity): OrderItemEntity {
        entity.orderId = EntityID(model.orderId, Orders)
        entity.productId = EntityID(model.productId, Products)
        entity.variantId = EntityID(model.variantId, ProductVariants)
        entity.warehouseId = model.warehouseId?.let { EntityID(it, Warehouses) }
        entity.quantity = model.quantity
        entity.unitPrice = model.unitPrice
        entity.totalPrice = model.totalPrice
        entity.customizationSnapshot = model.customizationSnapshot
        entity.productSnapshot = model.productSnapshot
        return entity
    }
}