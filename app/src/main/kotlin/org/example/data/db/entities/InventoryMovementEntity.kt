package org.example.data.db.entities

import org.example.data.db.config.CustomEntity
import org.example.data.db.config.CustomEntityClass
import org.example.data.db.tables.InventoryMovements
import org.jetbrains.exposed.v1.core.dao.id.EntityID

class InventoryMovementEntity(id: EntityID<String>) : CustomEntity(id, InventoryMovements) {
    companion object : CustomEntityClass<InventoryMovementEntity>(InventoryMovements)

    var variantId by InventoryMovements.variantId
    var warehouseId by InventoryMovements.warehouseId
    var movementType by InventoryMovements.movementType
    var quantity by InventoryMovements.quantity
    var referenceType by InventoryMovements.referenceType
    var referenceId by InventoryMovements.referenceId
    var performedBy by InventoryMovements.performedBy
    var notes by InventoryMovements.notes
}