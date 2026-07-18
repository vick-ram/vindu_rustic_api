package org.example.data.repo

import io.r2dbc.spi.ConnectionFactory
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import org.example.data.mappers.SupportTicketMapper
import org.example.di.Component
import org.example.di.Inject
import org.example.domain.models.support.SupportTicket
import org.koin.core.annotation.Single
import java.time.OffsetDateTime

@Component
class SupportTicketRepository @Inject constructor(
    connectionFactory: ConnectionFactory,
    supportTicketMapper: SupportTicketMapper
) : CrudRepository<SupportTicket, String>(
    connectionFactory = connectionFactory,
    tableName = "support_tickets",
    mapper = supportTicketMapper
) {
    override val generatedColumns = listOf("id", "created_at", "updated_at")

    // Override update to handle updated_at
    override suspend fun update(id: String, model: SupportTicket): SupportTicket? {
        validateTicket(model)
        return super.update(id, model)
    }

    // Get tickets by user
    suspend fun findByUserId(
        userId: String,
        offset: Int = 0,
        limit: Int = 20
    ): List<SupportTicket> {
        val sql = """
            SELECT * FROM $tableName 
            WHERE user_id = :userId 
            ORDER BY 
                CASE status 
                    WHEN 'open' THEN 1 
                    WHEN 'in_progress' THEN 2 
                    WHEN 'waiting' THEN 3 
                    ELSE 4 
                END,
                CASE priority 
                    WHEN 'urgent' THEN 1 
                    WHEN 'high' THEN 2 
                    WHEN 'normal' THEN 3 
                    WHEN 'low' THEN 4 
                    ELSE 5 
                END,
                created_at DESC 
            LIMIT :limit OFFSET :offset
        """.trimIndent()

        return executeQuery(sql, mapOf(
            "userId" to userId,
            "limit" to limit,
            "offset" to offset.toLong()
        ))
    }

    // Get tickets by status
    suspend fun findByStatus(
        status: String,
        offset: Int = 0,
        limit: Int = 20
    ): List<SupportTicket> {
        val sql = """
            SELECT * FROM $tableName 
            WHERE status = :status 
            ORDER BY 
                CASE priority 
                    WHEN 'urgent' THEN 1 
                    WHEN 'high' THEN 2 
                    WHEN 'normal' THEN 3 
                    WHEN 'low' THEN 4 
                    ELSE 5 
                END,
                created_at ASC 
            LIMIT :limit OFFSET :offset
        """.trimIndent()

        return executeQuery(sql, mapOf(
            "status" to status,
            "limit" to limit,
            "offset" to offset.toLong()
        ))
    }

    // Get tickets by priority
    suspend fun findByPriority(
        priority: String,
        offset: Int = 0,
        limit: Int = 20
    ): List<SupportTicket> {
        val sql = """
            SELECT * FROM $tableName 
            WHERE priority = :priority 
              AND status NOT IN ('resolved', 'closed') 
            ORDER BY created_at ASC 
            LIMIT :limit OFFSET :offset
        """.trimIndent()

        return executeQuery(sql, mapOf(
            "priority" to priority,
            "limit" to limit,
            "offset" to offset.toLong()
        ))
    }

    // Get tickets by type
    suspend fun findByTicketType(
        ticketType: String,
        offset: Int = 0,
        limit: Int = 20
    ): List<SupportTicket> {
        val sql = """
            SELECT * FROM $tableName 
            WHERE ticket_type = :ticketType 
            ORDER BY created_at DESC 
            LIMIT :limit OFFSET :offset
        """.trimIndent()

        return executeQuery(sql, mapOf(
            "ticketType" to ticketType,
            "limit" to limit,
            "offset" to offset.toLong()
        ))
    }

    // Get tickets assigned to a user
    suspend fun findByAssignedTo(
        assignedTo: String,
        includeResolved: Boolean = false,
        offset: Int = 0,
        limit: Int = 20
    ): List<SupportTicket> {
        val statusFilter = if (!includeResolved) {
            "AND status NOT IN ('resolved', 'closed')"
        } else ""

        val sql = """
            SELECT * FROM $tableName 
            WHERE assigned_to = :assignedTo 
            $statusFilter
            ORDER BY 
                CASE priority 
                    WHEN 'urgent' THEN 1 
                    WHEN 'high' THEN 2 
                    WHEN 'normal' THEN 3 
                    WHEN 'low' THEN 4 
                    ELSE 5 
                END,
                created_at ASC 
            LIMIT :limit OFFSET :offset
        """.trimIndent()

        return executeQuery(sql, mapOf(
            "assignedTo" to assignedTo,
            "limit" to limit,
            "offset" to offset.toLong()
        ))
    }

    // Get tickets by order
    suspend fun findByOrderId(orderId: String): List<SupportTicket> {
        val sql = """
            SELECT * FROM $tableName 
            WHERE order_id = :orderId 
            ORDER BY created_at DESC
        """.trimIndent()

        return executeQuery(sql, mapOf("orderId" to orderId))
    }

    // Update ticket status
    suspend fun updateStatus(
        id: String,
        status: String,
        resolvedAt: OffsetDateTime? = null
    ): SupportTicket? {
        val sql = """
            UPDATE $tableName 
            SET status = :status,
                updated_at = :updatedAt,
                resolved_at = CASE 
                    WHEN :status IN ('resolved', 'closed') THEN COALESCE(resolved_at, :resolvedAt)
                    ELSE resolved_at 
                END
            WHERE id = :id 
            RETURNING *
        """.trimIndent()

        return connectionFactory.withTransaction { connection ->
            connection.createStatement(sql)
                .bind("id", id)
                .bind("status", status)
                .bind("updatedAt", OffsetDateTime.now())
                .bind("resolvedAt", resolvedAt ?: OffsetDateTime.now())
                .execute()
                .awaitSingle()
                .map(rowMapper)
                .awaitFirstOrNull()
        }
    }

    // Assign ticket to support agent
    suspend fun assignTicket(id: String, assignedTo: String): SupportTicket? {
        val sql = """
            UPDATE $tableName 
            SET assigned_to = :assignedTo,
                status = CASE 
                    WHEN status = 'open' THEN 'in_progress' 
                    ELSE status 
                END,
                updated_at = :updatedAt
            WHERE id = :id 
            RETURNING *
        """.trimIndent()

        return connectionFactory.withTransaction { connection ->
            connection.createStatement(sql)
                .bind("id", id)
                .bind("assignedTo", assignedTo)
                .bind("updatedAt", OffsetDateTime.now())
                .execute()
                .awaitSingle()
                .map(rowMapper)
                .awaitFirstOrNull()
        }
    }

    // Search tickets
    suspend fun searchTickets(
        query: String,
        status: String? = null,
        priority: String? = null,
        offset: Int = 0,
        limit: Int = 20
    ): List<SupportTicket> {
        val conditions = mutableListOf("(subject ILIKE :query)")
        val params = mutableMapOf<String, Any>(
            "query" to "%$query%",
            "limit" to limit,
            "offset" to offset.toLong()
        )

        status?.let {
            conditions.add("status = :status")
            params["status"] = it
        }

        priority?.let {
            conditions.add("priority = :priority")
            params["priority"] = it
        }

        val sql = """
            SELECT * FROM $tableName 
            WHERE ${conditions.joinToString(" AND ")} 
            ORDER BY 
                CASE priority 
                    WHEN 'urgent' THEN 1 
                    WHEN 'high' THEN 2 
                    WHEN 'normal' THEN 3 
                    WHEN 'low' THEN 4 
                    ELSE 5 
                END,
                created_at DESC 
            LIMIT :limit OFFSET :offset
        """.trimIndent()

        return executeQuery(sql, params)
    }

    // Get ticket statistics
    suspend fun getTicketStats(
        assignedTo: String? = null
    ): TicketStats {
        val assignedFilter = if (assignedTo != null) "WHERE assigned_to = :assignedTo" else ""

        val sql = """
            SELECT 
                COUNT(*) as total_tickets,
                COUNT(CASE WHEN status = 'open' THEN 1 END) as open,
                COUNT(CASE WHEN status = 'in_progress' THEN 1 END) as in_progress,
                COUNT(CASE WHEN status = 'waiting' THEN 1 END) as waiting,
                COUNT(CASE WHEN status = 'resolved' THEN 1 END) as resolved,
                COUNT(CASE WHEN status = 'closed' THEN 1 END) as closed,
                COUNT(CASE WHEN priority = 'urgent' AND status NOT IN ('resolved', 'closed') THEN 1 END) as urgent,
                COUNT(CASE WHEN priority = 'high' AND status NOT IN ('resolved', 'closed') THEN 1 END) as high_priority,
                COALESCE(AVG(
                    EXTRACT(EPOCH FROM (resolved_at - created_at)) / 3600.0
                ), 0) as avg_resolution_hours
            FROM $tableName 
            $assignedFilter
        """.trimIndent()

        return connectionFactory.useConnection {
            val statement = createStatement(sql)
            if (assignedTo != null) {
                statement.bind("assignedTo", assignedTo)
            }

            statement.execute()
                .awaitSingle()
                .map { row, _ ->
                    TicketStats(
                        totalTickets = row.get("total_tickets", Long::class.java)!!.toInt(),
                        open = row.get("open", Long::class.java)!!.toInt(),
                        inProgress = row.get("in_progress", Long::class.java)!!.toInt(),
                        waiting = row.get("waiting", Long::class.java)!!.toInt(),
                        resolved = row.get("resolved", Long::class.java)!!.toInt(),
                        closed = row.get("closed", Long::class.java)!!.toInt(),
                        urgent = row.get("urgent", Long::class.java)!!.toInt(),
                        highPriority = row.get("high_priority", Long::class.java)!!.toInt(),
                        avgResolutionHours = row.get("avg_resolution_hours", Double::class.java) ?: 0.0
                    )
                }
                .awaitFirstOrNull() ?: TicketStats(0, 0, 0, 0, 0, 0, 0, 0, 0.0)
        }
    }

    // Get tickets with last message info
    suspend fun getTicketsWithLastMessage(
        userId: String? = null,
        assignedTo: String? = null,
        offset: Int = 0,
        limit: Int = 20
    ): List<TicketWithLastMessage> {
        val conditions = mutableListOf<String>()
        val params = mutableMapOf<String, Any>(
            "limit" to limit,
            "offset" to offset.toLong()
        )

        userId?.let {
            conditions.add("st.user_id = :userId")
            params["userId"] = it
        }

        assignedTo?.let {
            conditions.add("st.assigned_to = :assignedTo")
            params["assignedTo"] = it
        }

        val whereClause = if (conditions.isNotEmpty()) {
            "WHERE ${conditions.joinToString(" AND ")}"
        } else ""

        val sql = """
            SELECT 
                st.*,
                tm.message as last_message,
                tm.sender_id as last_sender_id,
                tm.created_at as last_message_at
            FROM $tableName st
            LEFT JOIN LATERAL (
                SELECT message, sender_id, created_at
                FROM ticket_messages
                WHERE ticket_id = st.id
                ORDER BY created_at DESC
                LIMIT 1
            ) tm ON true
            $whereClause
            ORDER BY 
                CASE st.priority 
                    WHEN 'urgent' THEN 1 
                    WHEN 'high' THEN 2 
                    WHEN 'normal' THEN 3 
                    WHEN 'low' THEN 4 
                    ELSE 5 
                END,
                COALESCE(tm.created_at, st.created_at) DESC 
            LIMIT :limit OFFSET :offset
        """.trimIndent()

        return connectionFactory.useConnection {
            val statement = createStatement(sql)
            params.forEach { (key, value) -> statement.bind(key, value) }

            statement.execute()
                .awaitSingle()
                .map { row, rowMetadata ->
                    TicketWithLastMessage(
                        ticket = rowMapper.apply(row, rowMetadata),
                        lastMessage = row.get("last_message", String::class.java),
                        lastSenderId = row.get("last_sender_id", String::class.java),
                        lastMessageAt = row.get("last_message_at", OffsetDateTime::class.java)
                    )
                }
                .asFlow()
                .toList()
        }
    }

    private fun validateTicket(ticket: SupportTicket) {
        if (ticket.userId.isBlank()) {
            throw IllegalArgumentException("User ID cannot be blank")
        }

        if (ticket.subject.isBlank()) {
            throw IllegalArgumentException("Subject cannot be blank")
        }

        val validStatuses = listOf("open", "in_progress", "waiting", "resolved", "closed")
        if (ticket.status !in validStatuses) {
            throw IllegalArgumentException("Invalid ticket status: ${ticket.status}")
        }

        val validPriorities = listOf("low", "normal", "high", "urgent")
        if (ticket.priority !in validPriorities) {
            throw IllegalArgumentException("Invalid ticket priority: ${ticket.priority}")
        }
    }
}

data class TicketStats(
    val totalTickets: Int,
    val open: Int,
    val inProgress: Int,
    val waiting: Int,
    val resolved: Int,
    val closed: Int,
    val urgent: Int,
    val highPriority: Int,
    val avgResolutionHours: Double
)

data class TicketWithLastMessage(
    val ticket: SupportTicket,
    val lastMessage: String?,
    val lastSenderId: String?,
    val lastMessageAt: OffsetDateTime?
)