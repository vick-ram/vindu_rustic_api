package org.example.data.db.tables

import org.example.data.db.config.CustomTable
import org.example.data.db.config.gsonJsonb

object TicketMessages : CustomTable("ticket_messages") {
    val ticketId = reference("ticket_id", SupportTickets)
    val senderId = reference("sender_id", Users)
    val message = text("message")
    val isInternal = bool("is_internal").default(false)
    val attachments = gsonJsonb<Map<String, Any>>("attachments").nullable()
}