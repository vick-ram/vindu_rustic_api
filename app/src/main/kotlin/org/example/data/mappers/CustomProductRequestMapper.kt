package org.example.data.mappers

import org.example.data.db.entities.CustomProductRequestEntity
import org.example.data.db.tables.Users
import org.example.domain.models.customization.CustomProductRequest
import org.example.domain.repo.EntityMapper
import org.jetbrains.exposed.v1.core.dao.id.EntityID

object CustomProductRequestMapper : EntityMapper<CustomProductRequestEntity, CustomProductRequest, String> {
    override fun toModel(entity: CustomProductRequestEntity): CustomProductRequest {
        return CustomProductRequest(
            id = entity.id.value,
            userId = entity.userId.value,
            title = entity.title,
            description = entity.description,
            specifications = entity.specifications,
            estimatedBudgetMin = entity.estimatedBudgetMin,
            estimatedBudgetMax = entity.estimatedBudgetMax,
            status = entity.status,
            notes = entity.notes,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }

    override fun toEntity(model: CustomProductRequest, entity: CustomProductRequestEntity): CustomProductRequestEntity {
        entity.userId = EntityID(model.userId, Users)
        entity.title = model.title
        entity.description = model.description
        entity.specifications = model.specifications
        entity.estimatedBudgetMin = model.estimatedBudgetMin
        entity.estimatedBudgetMax = model.estimatedBudgetMax
        entity.status = model.status
        entity.notes = model.notes
        return entity
    }
}