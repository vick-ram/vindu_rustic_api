package org.example.data.mappers

import org.example.data.db.entities.WarehouseEntity
import org.example.data.db.tables.Addresses
import org.example.domain.models.inventory.Warehouse
import org.example.domain.repo.EntityMapper
import org.jetbrains.exposed.v1.core.dao.id.EntityID

object WarehouseMapper : EntityMapper<WarehouseEntity, Warehouse, String> {
    override fun toModel(entity: WarehouseEntity): Warehouse {
        return Warehouse(
            id = entity.id.value,
            name = entity.name,
            addressId = entity.addressId?.value,
            isActive = entity.isActive,
            createdAt = entity.createdAt,
        )
    }

    override fun toEntity(model: Warehouse, entity: WarehouseEntity): WarehouseEntity {
        entity.name = model.name
        entity.addressId = model.addressId?.let { EntityID(it, Addresses) }
        entity.isActive = model.isActive
        return entity
    }
}