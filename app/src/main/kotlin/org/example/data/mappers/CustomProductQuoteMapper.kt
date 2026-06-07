package org.example.data.mappers

import org.example.data.db.entities.CustomProductQuoteEntity
import org.example.data.db.tables.CustomProductRequests
import org.example.data.db.tables.Users
import org.example.domain.models.customization.CustomProductQuote
import org.example.domain.repo.EntityMapper
import org.jetbrains.exposed.v1.core.dao.id.EntityID

object CustomProductQuoteMapper : EntityMapper<CustomProductQuoteEntity, CustomProductQuote, String> {
    override fun toModel(entity: CustomProductQuoteEntity): CustomProductQuote {
        return CustomProductQuote(
            id = entity.id.value,
            requestId = entity.requestId.value,
            quotedPrice = entity.quotedPrice,
            currency = entity.currency,
            productionTimeline = entity.productionTimelineDays,
            description = entity.description,
            validUntil = entity.validUntil,
            status = entity.status,
            createdBy = entity.createdBy.value,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }

    override fun toEntity(model: CustomProductQuote, entity: CustomProductQuoteEntity): CustomProductQuoteEntity {
        entity.requestId = EntityID(model.requestId, CustomProductRequests)
        entity.quotedPrice = model.quotedPrice
        entity.currency = model.currency
        entity.productionTimelineDays = model.productionTimeline
        entity.description = model.description
        entity.validUntil = model.validUntil
        entity.status = model.status
        entity.createdBy = EntityID(model.createdBy, Users)
        return entity
    }
}