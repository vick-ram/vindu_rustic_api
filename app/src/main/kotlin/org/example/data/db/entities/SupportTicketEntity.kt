package org.example.data.db.entities

import org.example.data.db.config.CustomEntity
import org.example.data.db.config.CustomEntityClass
import org.example.data.db.tables.SupportTickets
import org.example.data.db.tables.TicketMessages
import org.jetbrains.exposed.v1.core.dao.id.EntityID

class SupportTicketEntity(id: EntityID<String>) : CustomEntity(id, SupportTickets) {
    companion object : CustomEntityClass<SupportTicketEntity>(SupportTickets)

    var userId by SupportTickets.userId
    var orderId by SupportTickets.orderId
    var subject by SupportTickets.subject
    var status by SupportTickets.status
    var priority by SupportTickets.priority
    var ticketType by SupportTickets.ticketType
    var assignedTo by SupportTickets.assignedTo
    var resolvedAt by SupportTickets.resolvedAt

    val messages by TicketMessageEntity referrersOn TicketMessages.ticketId
}