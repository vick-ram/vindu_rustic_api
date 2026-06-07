package org.example.data.db.tables

import org.example.data.db.config.CustomTable
import org.jetbrains.exposed.v1.datetime.timestampWithTimeZone

object SupportTickets : CustomTable("support_tickets") {
    val userId = reference("user_id", Users)
    val orderId = reference("order_id", Orders).nullable()
    val subject = varchar("subject", 255)
    val status = varchar("status", 50).default("OPEN")
    val priority = varchar("priority", 50).default("NORMAL")
    val ticketType = varchar("ticket_type", 50).nullable()
    val assignedTo = reference("assigned_to", Users).nullable()
    val resolvedAt = timestampWithTimeZone("resolved_at").nullable()
}