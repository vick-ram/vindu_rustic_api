package org.example.data.mappers

import org.example.data.db.entities.OutboxEventEntity
import org.example.domain.models.system.OutboxEvent
import org.example.domain.repo.EntityMapper

object OutboxEventMapper : EntityMapper<OutboxEventEntity, OutboxEvent, String> {
    override fun toModel(entity: OutboxEventEntity): OutboxEvent {
        return OutboxEvent(
            id = entity.id.value,
            aggregateType = entity.aggregateType,
            aggregateId = entity.aggregateId,
            eventType = entity.eventType,
            payload = entity.payload,
            processed = entity.processed,
            processedAt = entity.processedAt,
            attempts = entity.attempts,
            errorMessage = entity.errorMessage,
            createdAt = entity.createdAt,
        )
    }

    override fun toEntity(model: OutboxEvent, entity: OutboxEventEntity): OutboxEventEntity {
        entity.aggregateType = model.aggregateType
        entity.aggregateId = model.aggregateId
        entity.eventType = model.eventType
        entity.payload = model.payload
        entity.processed = model.processed
        entity.processedAt = model.processedAt
        entity.attempts = model.attempts
        entity.errorMessage = model.errorMessage
        return entity
    }
}