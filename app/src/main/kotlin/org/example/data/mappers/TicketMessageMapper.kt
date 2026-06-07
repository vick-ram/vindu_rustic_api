package org.example.data.mappers

import org.example.data.db.entities.TicketMessageEntity
import org.example.data.db.tables.SupportTickets
import org.example.data.db.tables.Users
import org.example.domain.models.support.TicketMessage
import org.example.domain.repo.EntityMapper
import org.jetbrains.exposed.v1.core.dao.id.EntityID

object TicketMessageMapper : EntityMapper<TicketMessageEntity, TicketMessage, String> {
    override fun toModel(entity: TicketMessageEntity): TicketMessage {
        return TicketMessage(
            id = entity.id.value,
            ticketId = entity.ticketId.value,
            senderId = entity.senderId.value,
            message = entity.message,
            isInternal = entity.isInternal,
            attachments = entity.attachments,
            createdAt = entity.createdAt,
        )
    }

    override fun toEntity(model: TicketMessage, entity: TicketMessageEntity): TicketMessageEntity {
        entity.ticketId = EntityID(model.ticketId, SupportTickets)
        entity.senderId = EntityID(model.senderId, Users)
        entity.message = model.message
        entity.isInternal = model.isInternal
        entity.attachments = model.attachments
        return entity
    }
}