package org.example.data.mappers

import org.example.data.db.entities.ProductionUpdateEntity
import org.example.data.db.tables.ProductionJobs
import org.example.data.db.tables.Users
import org.example.domain.models.production.ProductionUpdate
import org.example.domain.repo.EntityMapper
import org.jetbrains.exposed.v1.core.dao.id.EntityID

object ProductionUpdateMapper : EntityMapper<ProductionUpdateEntity, ProductionUpdate, String> {
    override fun toModel(entity: ProductionUpdateEntity): ProductionUpdate {
        return ProductionUpdate(
            id = entity.id.value,
            jobId = entity.jobId.value,
            status = entity.status,
            stageName = entity.stageName,
            description = entity.description,
            imageUrl = entity.imageUrl,
            postedBy = entity.postedBy.value,
            createdAt = entity.createdAt,
        )
    }

    override fun toEntity(model: ProductionUpdate, entity: ProductionUpdateEntity): ProductionUpdateEntity {
        entity.jobId = EntityID(model.jobId, ProductionJobs)
        entity.status = model.status
        entity.stageName = model.stageName
        entity.description = model.description
        entity.imageUrl = model.imageUrl
        entity.postedBy = EntityID(model.postedBy, Users)
        return entity
    }
}