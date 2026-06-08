package org.example.domain.models.support

import kotlinx.datetime.LocalDateTime
import org.example.data.db.config.Ulid
import org.example.utils.now

data class Conversation(
    val id: String = Ulid.generate(),
    val lastMessageId: String? = null,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
)

data class Participant(
    val id: String = Ulid.generate(),
    val conversationId: String,
    val userId: String,
    val joinedAt: LocalDateTime = LocalDateTime.now(),
    val leftAt: LocalDateTime = LocalDateTime.now(),
    val isActive: Boolean = false
)

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

