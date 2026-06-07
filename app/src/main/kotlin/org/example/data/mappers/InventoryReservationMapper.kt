package org.example.data.mappers

import org.example.data.db.entities.InventoryReservationEntity
import org.example.data.db.tables.Orders
import org.example.data.db.tables.ProductVariants
import org.example.data.db.tables.ShoppingCarts
import org.example.data.db.tables.Warehouses
import org.example.domain.models.inventory.InventoryReservation
import org.example.domain.repo.EntityMapper
import org.jetbrains.exposed.v1.core.dao.id.EntityID

object InventoryReservationMapper : EntityMapper<InventoryReservationEntity, InventoryReservation, String> {
    override fun toModel(entity: InventoryReservationEntity): InventoryReservation {
        return InventoryReservation(
            id = entity.id.value,
            variantId = entity.variantId.value,
            warehouseId = entity.warehouseId.value,
            cartId = entity.cartId?.value,
            orderId = entity.orderId?.value,
            quantity = entity.quantity,
            status = entity.status,
            expiresAt = entity.expiresAt,
            createdAt = entity.createdAt,
        )
    }

    override fun toEntity(model: InventoryReservation, entity: InventoryReservationEntity): InventoryReservationEntity {
        entity.variantId = EntityID(model.variantId, ProductVariants)
        entity.warehouseId = EntityID(model.warehouseId, Warehouses)
        entity.cartId = model.cartId?.let { EntityID(it, ShoppingCarts) }
        entity.orderId = model.orderId?.let { EntityID(it, Orders) }
        entity.quantity = model.quantity
        entity.status = model.status
        entity.expiresAt = model.expiresAt
        return entity
    }
}