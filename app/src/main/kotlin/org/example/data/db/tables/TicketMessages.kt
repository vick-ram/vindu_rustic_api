package org.example.data.db.tables

import kotlinx.serialization.json.Json
import org.example.data.db.config.CustomTable
import org.jetbrains.exposed.v1.json.jsonb

object TicketMessages : CustomTable("ticket_messages") {
    val ticketId = reference("ticket_id", SupportTickets)
    val senderId = reference("sender_id", Users)
    val message = text("message")
    val isInternal = bool("is_internal").default(false)
    val attachments = jsonb<Map<String, Any>>("attachments", Json).nullable()
}