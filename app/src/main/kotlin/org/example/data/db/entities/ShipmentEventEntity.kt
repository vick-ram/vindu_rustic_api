package org.example.data.db.entities

import org.example.data.db.config.CustomEntity
import org.example.data.db.config.CustomEntityClass
import org.example.data.db.tables.ShipmentEvents
import org.jetbrains.exposed.v1.core.dao.id.EntityID

class ShipmentEventEntity(id: EntityID<String>) : CustomEntity(id, ShipmentEvents) {
    companion object : CustomEntityClass<ShipmentEventEntity>(ShipmentEvents)

    var shipmentId by ShipmentEvents.shipmentId
    var eventType by ShipmentEvents.eventType
    var status by ShipmentEvents.status
    var location by ShipmentEvents.location
    var description by ShipmentEvents.description
    var occurredAt by ShipmentEvents.occurredAt
}