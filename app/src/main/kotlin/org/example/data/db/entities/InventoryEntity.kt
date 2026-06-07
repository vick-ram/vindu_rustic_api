package org.example.data.db.entities

import org.example.data.db.config.CustomEntity
import org.example.data.db.config.CustomEntityClass
import org.example.data.db.tables.Inventories
import org.jetbrains.exposed.v1.core.dao.id.EntityID

class InventoryEntity(id: EntityID<String>) : CustomEntity(id, Inventories) {
    companion object : CustomEntityClass<InventoryEntity>(Inventories)

    var variantId by Inventories.variantId
    var warehouseId by Inventories.warehouseId
    var availableQuantity by Inventories.availableQuantity
    var reservedQuantity by Inventories.reservedQuantity
    var damagedQuantity by Inventories.damagedQuantity
    var lowStockThreshold by Inventories.lowStockThreshold
}