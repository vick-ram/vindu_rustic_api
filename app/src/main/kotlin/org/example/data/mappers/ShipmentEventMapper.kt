package org.example.data.mappers

import org.example.data.db.entities.ShipmentEventEntity
import org.example.data.db.tables.Shipments
import org.example.domain.models.shipping.ShipmentEvent
import org.example.domain.repo.EntityMapper
import org.jetbrains.exposed.v1.core.dao.id.EntityID

object ShipmentEventMapper : EntityMapper<ShipmentEventEntity, ShipmentEvent, String> {
    override fun toModel(entity: ShipmentEventEntity): ShipmentEvent {
        return ShipmentEvent(
            id = entity.id.value,
            shipmentId = entity.shipmentId.value,
            eventType = entity.eventType,
            status = entity.status,
            location = entity.location,
            description = entity.description,
            occurredAt = entity.occurredAt,
            createdAt = entity.createdAt,
        )
    }

    override fun toEntity(model: ShipmentEvent, entity: ShipmentEventEntity): ShipmentEventEntity {
        entity.shipmentId = EntityID(model.shipmentId, Shipments)
        entity.eventType = model.eventType
        entity.status = model.status
        entity.location = model.location
        entity.description = model.description
        entity.occurredAt = model.occurredAt
        return entity
    }
}