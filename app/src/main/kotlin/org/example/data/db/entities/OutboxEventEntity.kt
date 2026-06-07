package org.example.data.db.entities

import org.example.data.db.config.CustomEntity
import org.example.data.db.config.CustomEntityClass
import org.example.data.db.tables.OutboxEvents
import org.jetbrains.exposed.v1.core.dao.id.EntityID

class OutboxEventEntity(id: EntityID<String>) : CustomEntity(id, OutboxEvents) {
    companion object : CustomEntityClass<OutboxEventEntity>(OutboxEvents)

    var aggregateType by OutboxEvents.aggregateType
    var aggregateId by OutboxEvents.aggregateId
    var eventType by OutboxEvents.eventType
    var payload by OutboxEvents.payload
    var processed by OutboxEvents.processed
    var processedAt by OutboxEvents.processedAt
    var attempts by OutboxEvents.attempts
    var errorMessage by OutboxEvents.errorMessage
}