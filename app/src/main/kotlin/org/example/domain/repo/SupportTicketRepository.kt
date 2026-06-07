package org.example.domain.repo

import org.example.domain.models.support.SupportTicket

interface SupportTicketRepository : CrudRepository<SupportTicket, String> {
    suspend fun findByUserId(userId: String, offset: Int, limit: Int): List<SupportTicket>
    suspend fun findByStatus(status: String, offset: Int, limit: Int): List<SupportTicket>
    suspend fun assignTicket(ticketId: String, assigneeId: String): Boolean
    suspend fun resolveTicket(ticketId: String): Boolean
}