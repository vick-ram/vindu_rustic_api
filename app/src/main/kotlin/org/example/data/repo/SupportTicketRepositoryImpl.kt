package org.example.data.repo

import org.example.data.db.entities.SupportTicketEntity
import org.example.data.db.tables.SupportTickets
import org.example.data.db.tables.Users
import org.example.data.mappers.SupportTicketMapper
import org.example.domain.models.support.SupportTicket
import org.example.domain.repo.SupportTicketRepository
import org.example.plugins.NotFoundException
import org.example.utils.suspendTransaction
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import java.time.OffsetDateTime

class SupportTicketRepositoryImpl(private val ticketMapper: SupportTicketMapper) :
    CrudRepositoryImpl<SupportTicketEntity, SupportTicket>(SupportTicketEntity, SupportTicket::class),
    SupportTicketRepository {

    override suspend fun findByUserId(userId: String, offset: Int, limit: Int): List<SupportTicket> = suspendTransaction {
        SupportTicketEntity.find { SupportTickets.userId eq userId }
            .orderBy(SupportTickets.createdAt to SortOrder.DESC)
            .offset(offset.toLong())
            .limit(limit)
            .map { it.toDomain() }
    }

    override suspend fun findByStatus(status: String, offset: Int, limit: Int): List<SupportTicket> = suspendTransaction {
        SupportTicketEntity.find { SupportTickets.status eq status }
            .orderBy(SupportTickets.createdAt to SortOrder.DESC)
            .offset(offset.toLong())
            .limit(limit)
            .map { it.toDomain() }
    }

    override suspend fun assignTicket(ticketId: String, assigneeId: String): Boolean = suspendTransaction {
        val ticket = SupportTicketEntity.findById(ticketId) ?: throw NotFoundException("Ticket not found")
        ticket.assignedTo = EntityID(assigneeId, Users)
        ticket.status = "IN_PROGRESS"
        true
    }

    override suspend fun resolveTicket(ticketId: String): Boolean = suspendTransaction {
        val ticket = SupportTicketEntity.findById(ticketId) ?: throw NotFoundException("Ticket not found")
        ticket.status = "RESOLVED"
        ticket.resolvedAt = OffsetDateTime.now()
        true
    }

    override fun SupportTicketEntity.toDomain(): SupportTicket = ticketMapper.toModel(this)
    override fun SupportTicket.toEntity(entity: SupportTicketEntity) {
        ticketMapper.toEntity(this, entity)
    }
    override fun getId(domain: SupportTicket): String = domain.id.toString()
}