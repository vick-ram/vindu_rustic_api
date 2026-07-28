package org.example.data.repo

import io.ktor.client.utils.EmptyContent.status
import io.r2dbc.spi.Connection
import io.r2dbc.spi.ConnectionFactory
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.serialization.json.Json
import org.example.data.mappers.ConversationMapper
import org.example.data.mappers.MessageMapper
import org.example.data.mappers.ParticipantMapper
import org.example.di.Component
import org.example.di.Inject
import org.example.di.Qualifier
import org.example.domain.models.support.*
import org.example.utils.Ulid
import java.time.OffsetDateTime

@Component
class ConversationRepository @Inject constructor(
    connectionFactory: ConnectionFactory,
    private val conversationMapper: ConversationMapper,
    private val participantMapper: ParticipantMapper,
    private val messageMapper: MessageMapper
) :
    CrudRepository<Conversation, String>(
        connectionFactory = connectionFactory, tableName = "conversations", mapper = conversationMapper
    ) {

    suspend fun findOrCreateConversation(
        userId: String,
        otherUserId: String
    ): Conversation {
        return findConversationBetweenUsers(userId, otherUserId)
            ?: createConversation(userId, otherUserId)
    }

    suspend fun getUserConversations(userId: String): List<Conversation> {
        val sql = """
            SELECT c.*, 
                   m.id as last_message_id,
                   m.message_text as last_message_text,
                   m.sender_id as last_message_sender_id,
                   m.message_type as last_message_type,
                   m.created_at as last_message_created_at,
                   COALESCE(
                       json_agg(p.*) FILTER (WHERE p.id IS NOT NULL), '[]'
                   ) as participants_json
            FROM conversations c
            INNER JOIN participants p ON c.id = p.conversation_id AND p.is_active = true
            LEFT JOIN messages m ON c.last_message_id = m.id
            WHERE c.id IN (SELECT conversation_id FROM participants WHERE user_id = $1 AND is_active = true)
            GROUP BY c.id, m.id
            ORDER BY c.updated_at DESC
        """.trimIndent()

        return executeQuery(sql, mapOf("$1" to userId)) { row, metadata ->
            val conversation = conversationMapper.toModel(row, metadata)

            val lastMessage = row.get("last_message_id", String::class.java)?.let { id ->
                Message(
                    id = id,
                    conversationId = conversation.id,
                    senderId = row.get("last_message_sender_id", String::class.java)!!,
                    messageText = row.get("last_message_text", String::class.java) ?: "",
                    messageType = MessageType.valueOf(row.get("last_message_type", String::class.java)!!),
                    createdAt = row.get("last_message_created_at", OffsetDateTime.now()::class.java)!!
                )
            }

            val participantsRawJson = row.get("participants_json", String::class.java)
            val participants = participantsRawJson?.let { Json.decodeFromString<List<Participant>>(it) }
            conversation.copy(lastMessage = lastMessage, participants = participants ?: emptyList())
        }
    }

    private suspend fun findConversationBetweenUsers(
        userId: String,
        otherUserId: String
    ): Conversation? {
        val sql = """
            SELECT c.*
            FROM conversations c
            INNER JOIN participants p1 ON c.id = p1.conversation_id 
                AND p1.user_id = :userId 
                AND p1.is_active = true
            INNER JOIN participants p2 ON c.id = p2.conversation_id 
                AND p2.user_id = :otherUserId 
                AND p2.is_active = true
            LIMIT 1
        """.trimIndent()

        val conversations = executeQuery(
            sql = sql,
            params = mapOf("userId" to userId, "otherUserId" to otherUserId),
            mapper = rowMapper
        )

        return conversations.firstOrNull()?.let { conversation ->
            val participants = getConversationParticipants(conversation.id)
            conversation.copy(participants = participants)
        }
    }

    private suspend fun createConversation(
        userId: String,
        otherUserId: String
    ): Conversation = connectionFactory.withTransaction { connection ->
        val conversationId = Ulid.generate()
        val now = OffsetDateTime.now()
        val conversationSql = """
            INSERT INTO conversations (id, created_at, updated_at) 
            VALUES (:id, :createdAt, :updatedAt)
            RETURNING *
        """.trimIndent()

        // Insert conversation
        connection.createNamedStatement(
            conversationSql,
            mapOf("id" to conversationId, "createdAt" to now, "updatedAt" to now)
        )
            .execute()
            .awaitSingle()
            .map(rowMapper)
            .awaitSingle()

        val participantSql = """
                INSERT INTO participants (id, conversation_id, user_id, joined_at, is_active) 
                VALUES (:id, :conversationId, :userId, :joinedAt, :isActive)
            """.trimIndent()

        // Insert participants
        listOf(userId, otherUserId).forEach { participantUserId ->
            connection.createNamedStatement(
                participantSql,
                mapOf(
                    "conversationId" to conversationId,
                    "userId" to participantUserId,
                    "joinedAt" to now,
                    "isActive" to true
                )
            )
                .execute()
                .awaitSingle()
                .rowsUpdated
                .awaitSingle()
        }

        // Return created conversation with participants
        val conversation = read(conversationId)!!
        val participants = getConversationParticipants(conversationId)
        conversation.copy(participants = participants)
    }

    private suspend fun getConversationParticipants(conversationId: String): List<Participant> {
        return executeQuery(
            sql = """
                SELECT * FROM participants 
                WHERE conversation_id = :conversationId AND is_active = true
            """.trimIndent(),
            params = mapOf("conversationId" to conversationId),
            mapper = { row, metadata -> participantMapper.toModel(row, metadata) }
        )
    }
}


class ParticipantRepository(
    connectionFactory: ConnectionFactory,
    participantMapper: ParticipantMapper
) : CrudRepository<Participant, String>(
    connectionFactory = connectionFactory,
    tableName = "participants",
    mapper = participantMapper
) {
    suspend fun getConversationParticipants(conversationId: String): List<Participant> {
        return executeQuery(
            sql = "SELECT * FROM participants WHERE conversation_id = :id AND is_active = true",
            params = mapOf("id" to conversationId),
            mapper = rowMapper
        )
    }

    suspend fun getUserActiveConversations(userId: String): List<Participant> {
        return executeQuery(
            sql = "SELECT * FROM participants WHERE user_id = :userId AND is_active = true",
            params = mapOf("userId" to userId),
            mapper = rowMapper
        )
    }
}

class MessageRepository(
    connectionFactory: ConnectionFactory,
    private val messageMapper: MessageMapper,
    private val participantMapper: ParticipantMapper
) : CrudRepository<Message, String>(
    connectionFactory = connectionFactory,
    tableName = "messages",
    mapper = messageMapper
) {

    suspend fun sendMessage(
        conversationId: String,
        senderId: String,
        text: String,
        type: MessageType
    ): Message = connectionFactory.withTransaction { connection ->
        val messageId = Ulid.generate()
        val now = OffsetDateTime.now()
        val messageSql = """
            INSERT INTO messages (id, conversation_id, sender_id, message_text, message_type, created_at, updated_at)
            VALUES (:id, :conversationId, :senderId, :text, :type, :createdAt, :updatedAt)
            RETURNING *
        """.trimIndent()
        val messageParams = mapOf(
            "id" to messageId,
            "conversationId" to conversationId,
            "senderId" to senderId,
            "text" to text,
            "type" to type,
            "createdAt" to now,
            "updatedAt" to now
        )

        // Insert message
        connection.createNamedStatement(messageSql, messageParams)
            .execute()
            .awaitSingle()
            .map(rowMapper)
            .awaitSingle()

        // Get active participants (excluding sender) - using existing executeQuery
        val participants = getActiveParticipants(connection, conversationId, senderId)

        val messageStatusSql = """
                INSERT INTO message_status (id, message_id, user_id, status, updated_at)
                VALUES (:id, :messageId, :userId, :status, :updatedAt)
            """.trimIndent()

        // Create message status for each participant
        participants.forEach { participant ->
            val messageStatusParams = mapOf(
                "id" to Ulid.generate(),
                "messageId" to messageId,
                "userId" to participant.userId,
                "status" to MessageStatus.SENT.name,
                "updatedAt" to now
            )

            connection.createNamedStatement(messageStatusSql, messageStatusParams)
                .execute()
                .awaitSingle()
                .rowsUpdated
                .awaitSingle()
        }

        val conversationSql = """
            UPDATE conversations 
            SET updated_at = :updatedAt, last_message_id = :messageId
            WHERE id = :conversationId
        """.trimIndent()

        // Update conversation's last message and timestamp
        connection.createNamedStatement(conversationSql, mapOf(
            "conversationId" to conversationId,
            "messageId" to messageId,
            "updatedAt" to now
        ))
            .execute()
            .awaitSingle()
            .rowsUpdated
            .awaitSingle()

        // Return created message
        read(messageId)!!
    }

    suspend fun getConversationMessages(
        conversationId: String,
        currentUserId: String
    ): List<Message> {
        return executeQuery(
            sql = """
                SELECT m.*, 
                       COALESCE(ms.status, 'SENT') as user_status
                FROM messages m
                LEFT JOIN message_status ms 
                    ON m.id = ms.message_id AND ms.user_id = :currentUserId
                WHERE m.conversation_id = :conversationId
                ORDER BY m.created_at ASC
            """.trimIndent(),
            params = mapOf(
                "conversationId" to conversationId,
                "currentUserId" to currentUserId
            ),
            mapper = { row, metadata ->
                val message = messageMapper.toModel(row, metadata)
                val status = row.get("user_status", String::class.java)?.let {
                    try {
                        MessageStatus.valueOf(it)
                    } catch (e: Exception) {
                        null
                    }
                }
                message.copy(status = status)
            }
        )
    }

    suspend fun markMessageAsRead(messageId: String, userId: String) {
        connectionFactory.withTransaction { connection ->
            val now = OffsetDateTime.now()

            // Upsert message status using PostgreSQL ON CONFLICT
            connection.createNamedStatement(
                """
                INSERT INTO message_status (id, message_id, user_id, status, updated_at)
                VALUES (:id, :messageId, :userId, :status, :updatedAt)
                ON CONFLICT (message_id, user_id) 
                DO UPDATE SET status = :status, updated_at = :updatedAt
            """.trimIndent(),
                mapOf(
                    "id" to Ulid.generate(),
                    "messageId" to messageId,
                    "userId" to userId,
                    "status" to MessageStatus.READ.name,
                    "updatedAt" to now
                )
            )
                .execute()
                .awaitSingle()
                .rowsUpdated
                .awaitSingle()
        }
    }

    suspend fun getUnreadCount(conversationId: String, userId: String): Int {
        val result = executeQuery(
            sql = """
                SELECT COUNT(*) as unread_count
                FROM messages m
                LEFT JOIN message_status ms 
                    ON m.id = ms.message_id AND ms.user_id = :userId
                WHERE m.conversation_id = :conversationId
                AND (ms.status IS NULL OR ms.status != 'READ')
            """.trimIndent(),
            params = mapOf(
                "conversationId" to conversationId,
                "userId" to userId
            ),
            mapper = { row, _ ->
                row.get("unread_count", Long::class.java)?.toInt() ?: 0
            }
        )
        return result.firstOrNull() ?: 0
    }

    suspend fun markConversationMessagesAsRead(
        conversationId: String,
        userId: String
    ) = connectionFactory.withTransaction { connection ->
        val now = OffsetDateTime.now()

        // Single query to mark all messages as read using PostgreSQL's INSERT...ON CONFLICT
        connection.createNamedStatement(
            """
            INSERT INTO message_status (id, message_id, user_id, status, updated_at)
            SELECT 
                gen_random_uuid(),
                m.id,
                :userId,
                :status,
                :updatedAt
            FROM messages m
            LEFT JOIN message_status ms 
                ON m.id = ms.message_id AND ms.user_id = :userId
            WHERE m.conversation_id = :conversationId
            AND (ms.status IS NULL OR ms.status != 'READ')
            ON CONFLICT (message_id, user_id) 
            DO UPDATE SET status = :status, updated_at = :updatedAt
        """.trimIndent(),
            mapOf(
                "conversationId" to conversationId,
                "userId" to userId,
                "status" to MessageStatus.READ.name,
                "updatedAt" to now
            )
        )
            .execute()
            .awaitSingle()
            .rowsUpdated
            .awaitSingle()
        return@withTransaction
    }

    private suspend fun getActiveParticipants(
        connection: Connection,
        conversationId: String,
        excludeUserId: String
    ): List<Participant> {
        return connection.createNamedStatement(
            """
            SELECT * FROM participants 
            WHERE conversation_id = :conversationId 
            AND is_active = true 
            AND user_id != :excludeUserId
        """.trimIndent(),
            mapOf(
                "conversationId" to conversationId,
                "excludeUserId" to excludeUserId
            )
        )
            .execute()
            .awaitSingle()
            .map { row, metadata -> participantMapper.toModel(row, metadata) }
            .asFlow()
            .toList()
    }
}



