package org.example.data.mappers

import org.example.data.db.entities.InventoryMovementEntity
import org.example.data.db.tables.ProductVariants
import org.example.data.db.tables.Users
import org.example.data.db.tables.Warehouses
import org.example.domain.models.inventory.InventoryMovement
import org.example.domain.repo.EntityMapper
import org.jetbrains.exposed.v1.core.dao.id.EntityID

object InventoryMovementMapper : EntityMapper<InventoryMovementEntity, InventoryMovement, String> {
    override fun toModel(entity: InventoryMovementEntity): InventoryMovement {
        return InventoryMovement(
            id = entity.id.value,
            variantId = entity.variantId.value,
            warehouseId = entity.warehouseId.value,
            movementType = entity.movementType,
            quantity = entity.quantity,
            referenceType = entity.referenceType,
            referenceId = entity.referenceId,
            performedBy = entity.performedBy?.value,
            notes = entity.notes,
            createdAt = entity.createdAt,
        )
    }

    override fun toEntity(model: InventoryMovement, entity: InventoryMovementEntity): InventoryMovementEntity {
        entity.variantId = EntityID(model.variantId, ProductVariants)
        entity.warehouseId = EntityID(model.warehouseId, Warehouses)
        entity.movementType = model.movementType
        entity.quantity = model.quantity
        entity.referenceType = model.referenceType
        entity.referenceId = model.referenceId
        entity.performedBy = model.performedBy?.let { EntityID(it, Users) }
        entity.notes = model.notes
        return entity
    }
}