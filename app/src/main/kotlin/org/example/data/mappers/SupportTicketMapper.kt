package org.example.data.mappers

import org.example.data.db.entities.SupportTicketEntity
import org.example.data.db.tables.Orders
import org.example.data.db.tables.Users
import org.example.domain.models.support.SupportTicket
import org.example.domain.repo.EntityMapper
import org.jetbrains.exposed.v1.core.dao.id.EntityID

object SupportTicketMapper : EntityMapper<SupportTicketEntity, SupportTicket, String> {
    override fun toModel(entity: SupportTicketEntity): SupportTicket {
        return SupportTicket(
            id = entity.id.value,
            userId = entity.userId.value,
            orderId = entity.orderId?.value,
            subject = entity.subject,
            status = entity.status,
            priority = entity.priority,
            ticketType = entity.ticketType,
            assignedTo = entity.assignedTo?.value,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            resolvedAt = entity.resolvedAt
        )
    }

    override fun toEntity(model: SupportTicket, entity: SupportTicketEntity): SupportTicketEntity {
        entity.userId = EntityID(model.userId, Users)
        entity.orderId = model.orderId?.let { EntityID(it, Orders) }
        entity.subject = model.subject
        entity.status = model.status
        entity.priority = model.priority
        entity.ticketType = model.ticketType
        entity.assignedTo = model.assignedTo?.let { EntityID(it, Users) }
        entity.resolvedAt = model.resolvedAt
        return entity
    }
}