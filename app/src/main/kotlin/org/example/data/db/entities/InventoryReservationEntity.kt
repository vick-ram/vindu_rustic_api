package org.example.data.db.entities

import org.example.data.db.config.CustomEntity
import org.example.data.db.config.CustomEntityClass
import org.example.data.db.tables.InventoryReservations
import org.jetbrains.exposed.v1.core.dao.id.EntityID

class InventoryReservationEntity(id: EntityID<String>) : CustomEntity(id, InventoryReservations) {
    companion object : CustomEntityClass<InventoryReservationEntity>(InventoryReservations)

    var variantId by InventoryReservations.variantId
    var warehouseId by InventoryReservations.warehouseId
    var cartId by InventoryReservations.cartId
    var orderId by InventoryReservations.orderId
    var quantity by InventoryReservations.quantity
    var status by InventoryReservations.status
    var expiresAt by InventoryReservations.expiresAt
}