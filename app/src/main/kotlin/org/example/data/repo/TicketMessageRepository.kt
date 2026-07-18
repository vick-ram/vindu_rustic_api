package org.example.data.repo

import io.r2dbc.spi.ConnectionFactory
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.serialization.Serializable
import org.example.data.mappers.TicketMessageMapper
import org.example.di.Component
import org.example.di.Inject
import org.example.domain.models.support.TicketMessage
import org.koin.core.annotation.Single
import java.time.OffsetDateTime

@Component
class TicketMessageRepository @Inject constructor(
    connectionFactory: ConnectionFactory,
    ticketMessageMapper: TicketMessageMapper
) : CrudRepository<TicketMessage, String>(
    connectionFactory = connectionFactory,
    tableName = "ticket_messages",
    idColumn = "id",
    mapper = ticketMessageMapper
) {
    override val generatedColumns = listOf("id", "created_at")

    // Get messages for a ticket
    suspend fun findByTicketId(
        ticketId: String,
        includeInternal: Boolean = false,
        offset: Int = 0,
        limit: Int = 50
    ): List<TicketMessage> {
        val internalFilter = if (!includeInternal) "AND is_internal = false" else ""

        val sql = """
            SELECT * FROM $tableName 
            WHERE ticket_id = :ticketId 
            $internalFilter
            ORDER BY created_at ASC 
            LIMIT :limit OFFSET :offset
        """.trimIndent()

        return executeQuery(sql, mapOf(
            "ticketId" to ticketId,
            "limit" to limit,
            "offset" to offset.toLong()
        ))
    }

    // Get messages by sender
    suspend fun findBySenderId(
        senderId: String,
        offset: Int = 0,
        limit: Int = 20
    ): List<TicketMessage> {
        val sql = """
            SELECT * FROM $tableName 
            WHERE sender_id = :senderId 
            ORDER BY created_at DESC 
            LIMIT :limit OFFSET :offset
        """.trimIndent()

        return executeQuery(sql, mapOf(
            "senderId" to senderId,
            "limit" to limit,
            "offset" to offset.toLong()
        ))
    }

    // Get latest message for a ticket
    suspend fun getLatestMessage(ticketId: String): TicketMessage? {
        val sql = """
            SELECT * FROM $tableName 
            WHERE ticket_id = :ticketId 
            ORDER BY created_at DESC 
            LIMIT 1
        """.trimIndent()

        return connectionFactory.useConnection {
            createStatement(sql)
                .bind("ticketId", ticketId)
                .execute()
                .awaitSingle()
                .map(rowMapper)
                .awaitFirstOrNull()
        }
    }

    // Get message count for a ticket
    suspend fun getMessageCount(
        ticketId: String,
        includeInternal: Boolean = false
    ): Long {
        val internalFilter = if (!includeInternal) "AND is_internal = false" else ""

        val sql = """
            SELECT COUNT(*) as count 
            FROM $tableName 
            WHERE ticket_id = :ticketId 
            $internalFilter
        """.trimIndent()

        return connectionFactory.useConnection {
            createStatement(sql)
                .bind("ticketId", ticketId)
                .execute()
                .awaitSingle()
                .map { row, _ -> row.get("count", Long::class.java) }
                .awaitFirstOrNull() ?: 0L
        }
    }

    // Add internal note
    suspend fun addInternalNote(
        ticketId: String,
        senderId: String,
        message: String,
        attachments: Map<String, Any>? = null
    ): TicketMessage {
        return create(
            TicketMessage(
                ticketId = ticketId,
                senderId = senderId,
                message = message,
                isInternal = true,
                attachments = attachments
            )
        )
    }

    // Add customer message
    suspend fun addCustomerMessage(
        ticketId: String,
        senderId: String,
        message: String,
        attachments: Map<String, Any>? = null
    ): TicketMessage {
        return create(
            TicketMessage(
                ticketId = ticketId,
                senderId = senderId,
                message = message,
                isInternal = false,
                attachments = attachments
            )
        )
    }

    // Get message thread with sender info
    suspend fun getMessageThread(
        ticketId: String,
        includeInternal: Boolean = false
    ): List<MessageWithSender> {
        val internalFilter = if (!includeInternal) "AND tm.is_internal = false" else ""

        val sql = """
            SELECT 
                tm.*,
                u.name as sender_name,
                u.email as sender_email,
                u.avatar_url as sender_avatar
            FROM $tableName tm
            LEFT JOIN users u ON tm.sender_id = u.id
            WHERE tm.ticket_id = :ticketId 
            $internalFilter
            ORDER BY tm.created_at ASC
        """.trimIndent()

        return connectionFactory.useConnection {
            createStatement(sql)
                .bind("ticketId", ticketId)
                .execute()
                .awaitSingle()
                .map { row, rowMetadata ->
                    MessageWithSender(
                        message = rowMapper.apply(row, rowMetadata),
                        senderName = row.get("sender_name", String::class.java),
                        senderEmail = row.get("sender_email", String::class.java),
                        senderAvatar = row.get("sender_avatar", String::class.java)
                    )
                }
                .asFlow()
                .toList()
        }
    }

    // Get messages by date range
    suspend fun findByDateRange(
        startDate: OffsetDateTime,
        endDate: OffsetDateTime,
        ticketId: String? = null,
        offset: Int = 0,
        limit: Int = 50
    ): List<TicketMessage> {
        val ticketFilter = if (ticketId != null) "AND ticket_id = :ticketId" else ""

        val sql = """
            SELECT * FROM $tableName 
            WHERE created_at BETWEEN :startDate AND :endDate 
            $ticketFilter
            ORDER BY created_at DESC 
            LIMIT :limit OFFSET :offset
        """.trimIndent()

        val params = mutableMapOf(
            "startDate" to startDate,
            "endDate" to endDate,
            "limit" to limit,
            "offset" to offset.toLong()
        )

        if (ticketId != null) {
            params["ticketId"] = ticketId
        }

        return executeQuery(sql, params)
    }

    // Get unread messages count for a user
    suspend fun getUnreadCount(userId: String): Long {
        val sql = """
            SELECT COUNT(*) as count 
            FROM $tableName tm
            INNER JOIN support_tickets st ON tm.ticket_id = st.id
            WHERE st.user_id = :userId 
              AND tm.sender_id != :userId 
              AND tm.is_internal = false
              AND tm.created_at > COALESCE(
                  (SELECT MAX(tm2.created_at) 
                   FROM ticket_messages tm2 
                   WHERE tm2.ticket_id = tm.ticket_id 
                     AND tm2.sender_id = :userId),
                  '1970-01-01'::timestamp
              )
        """.trimIndent()

        return connectionFactory.useConnection {
            createStatement(sql)
                .bind("userId", userId)
                .execute()
                .awaitSingle()
                .map { row, _ -> row.get("count", Long::class.java) }
                .awaitFirstOrNull() ?: 0L
        }
    }
}

@Serializable
data class MessageWithSender(
    val message: TicketMessage,
    val senderName: String?,
    val senderEmail: String?,
    val senderAvatar: String?
)