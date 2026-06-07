package org.example.data.db.entities

import org.example.data.db.config.CustomEntity
import org.example.data.db.config.CustomEntityClass
import org.example.data.db.tables.Inventories
import org.example.data.db.tables.Warehouses
import org.jetbrains.exposed.v1.core.dao.id.EntityID

class WarehouseEntity(id: EntityID<String>) : CustomEntity(id, Warehouses) {
    companion object : CustomEntityClass<WarehouseEntity>(Warehouses)

    var name by Warehouses.name
    var addressId by Warehouses.addressId
    var isActive by Warehouses.isActive

    val inventory by InventoryEntity referrersOn Inventories.warehouseId
}