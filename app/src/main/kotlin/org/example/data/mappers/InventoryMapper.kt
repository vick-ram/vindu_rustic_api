package org.example.data.mappers

import org.example.data.db.entities.InventoryEntity
import org.example.data.db.tables.ProductVariants
import org.example.data.db.tables.Warehouses
import org.example.domain.models.inventory.Inventory
import org.example.domain.repo.EntityMapper
import org.jetbrains.exposed.v1.core.dao.id.EntityID

object InventoryMapper : EntityMapper<InventoryEntity, Inventory, String> {
    override fun toModel(entity: InventoryEntity): Inventory {
        return Inventory(
            id = entity.id.value,
            variantId = entity.variantId.value,
            warehouseId = entity.warehouseId.value,
            availableQuantity = entity.availableQuantity,
            reservedQuantity = entity.reservedQuantity,
            damagedQuantity = entity.damagedQuantity,
            lowStockThreshold = entity.lowStockThreshold,
            updatedAt = entity.updatedAt
        )
    }

    override fun toEntity(model: Inventory, entity: InventoryEntity): InventoryEntity {
        entity.variantId = EntityID(model.variantId, ProductVariants)
        entity.warehouseId = EntityID(model.warehouseId, Warehouses)
        entity.availableQuantity = model.availableQuantity
        entity.reservedQuantity = model.reservedQuantity
        entity.damagedQuantity = model.damagedQuantity
        entity.lowStockThreshold = model.lowStockThreshold
        return entity
    }
}