package org.example.domain.models.support

import kotlinx.serialization.Serializable
import org.example.data.db.config.Ulid
import java.time.LocalDateTime

@Serializable
data class Conversation(
    val id: String = Ulid.generate(),
    val lastMessageId: String? = null,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
)

@Serializable
data class Participant(
    val id: String = Ulid.generate(),
    val conversationId: String,
    val userId: String,
    val joinedAt: LocalDateTime = LocalDateTime.now(),
    val leftAt: LocalDateTime = LocalDateTime.now(),
    val isActive: Boolean = false
)

@Serializable
data class Message (
    val id: String = Ulid.generate(),
    val conversationId: String,
    val senderId: String,
    val messageText: String,
    val messageType: MessageType,
    val status: MessageStatus = MessageStatus.SENT,
    val parentMessageId: String? = null, // For self reply
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val fileUrl: String?,
    val fileName: String?,
    val fileSize: Long?
)

enum class MessageType { TEXT, IMAGE, VIDEO, FILE }

enum class MessageStatus { SENT, DELIVERED, READ }

