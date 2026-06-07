package org.example.data.db.entities

import org.example.data.db.config.CustomEntity
import org.example.data.db.config.CustomEntityClass
import org.example.data.db.tables.TicketMessages
import org.jetbrains.exposed.v1.core.dao.id.EntityID

class TicketMessageEntity(id: EntityID<String>) : CustomEntity(id, TicketMessages) {
    companion object : CustomEntityClass<TicketMessageEntity>(TicketMessages)

    var ticketId by TicketMessages.ticketId
    var senderId by TicketMessages.senderId
    var message by TicketMessages.message
    var isInternal by TicketMessages.isInternal
    var attachments by TicketMessages.attachments
}