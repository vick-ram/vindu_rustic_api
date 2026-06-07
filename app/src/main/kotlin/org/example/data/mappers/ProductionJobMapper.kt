package org.example.data.mappers

import org.example.data.db.entities.ProductionJobEntity
import org.example.data.db.tables.OrderItems
import org.example.data.db.tables.Products
import org.example.data.db.tables.Users
import org.example.domain.models.production.ProductionJob
import org.example.domain.repo.EntityMapper
import org.jetbrains.exposed.v1.core.dao.id.EntityID

object ProductionJobMapper : EntityMapper<ProductionJobEntity, ProductionJob, String> {
    override fun toModel(entity: ProductionJobEntity): ProductionJob {
        return ProductionJob(
            id = entity.id.value,
            orderItemId = entity.orderItemId.value,
            productId = entity.productId.value,
            assignedTo = entity.assignedTo?.value,
            status = entity.status,
            priority = entity.priority,
            quantity = entity.quantity,
            notes = entity.notes,
            startedAt = entity.startedAt,
            completedAt = entity.completedAt,
            estimatedCompletionAt = entity.estimatedCompletionAt,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }

    override fun toEntity(model: ProductionJob, entity: ProductionJobEntity): ProductionJobEntity {
        entity.orderItemId = EntityID(model.orderItemId, OrderItems)
        entity.productId = EntityID(model.productId, Products)
        entity.assignedTo = model.assignedTo?.let { EntityID(it, Users) }
        entity.status = model.status
        entity.priority = model.priority
        entity.quantity = model.quantity
        entity.notes = model.notes
        entity.startedAt = model.startedAt
        entity.completedAt = model.completedAt
        entity.estimatedCompletionAt = model.estimatedCompletionAt
        return entity
    }
}