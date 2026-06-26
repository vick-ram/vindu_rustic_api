package org.example.domain.models.support

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.example.data.db.config.Ulid
import java.time.OffsetDateTime

@Serializable
data class Conversation(
    val id: String = Ulid.generate(),
    val participants: List<Participant> = emptyList(),

    @SerialName("last_message")
    val lastMessage: Message? = null,

    @Contextual
    @SerialName("created_at")
    val createdAt: OffsetDateTime = OffsetDateTime.now(),

    @Contextual
    @SerialName("updated_at")
    val updatedAt: OffsetDateTime = OffsetDateTime.now()
)

@Serializable
data class Participant(
    val id: String = Ulid.generate(),

    @SerialName("user_id")
    val userId: String,

    @SerialName("conversation_id")
    val conversationId: String,

    @Contextual
    @SerialName("joined_at")
    val joinedAt: OffsetDateTime = OffsetDateTime.now(),

    @Contextual
    @SerialName("left_at")
    val leftAt: OffsetDateTime? = null,
    val isActive: Boolean = false
)

@Serializable
data class Message (
    val id: String = Ulid.generate(),

    @SerialName("conversation_id")
    val conversationId: String,

    @SerialName("sender_id")
    val senderId: String,
    val messageText: String,
    val messageType: MessageType,
    val status: MessageStatus? = null,
    val parentMessageId: String? = null, // For self reply

    @Contextual
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
    val fileUrl: String? = null,
    val fileName: String? = null,
    val fileSize: Long? = null
)

enum class MessageType { TEXT, IMAGE, VIDEO, FILE }

enum class MessageStatus { SENT, DELIVERED, READ }

