package org.example.data.repo

import kotlinx.datetime.LocalDateTime
import org.example.data.db.entities.*
import org.example.data.db.tables.MessageStatusTable
import org.example.data.db.tables.MessagesTable
import org.example.data.db.tables.ParticipantsTable
import org.example.data.mappers.ConversationMapper
import org.example.data.mappers.MessageMapper
import org.example.data.mappers.ParticipantMapper
import org.example.domain.models.support.Conversation
import org.example.domain.models.support.Message
import org.example.domain.models.support.MessageStatus
import org.example.domain.models.support.MessageType
import org.example.domain.models.support.Participant
import org.example.domain.repo.ConversationRepository
import org.example.domain.repo.MessageRepository
import org.example.utils.now
import org.example.utils.suspendTransaction
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.statements.UpsertSqlExpressionBuilder.eq

class ConversationRepositoryImpl(private val conversationMapper: ConversationMapper) :
    CrudRepositoryImpl<ConversationEntity, Conversation>(
        ConversationEntity, Conversation::class
    ), ConversationRepository {
    override fun ConversationEntity.toDomain(): Conversation {
        return conversationMapper.toModel(this)
    }

    override fun Conversation.toEntity(entity: ConversationEntity) {
        conversationMapper.toEntity(this, entity)
    }

    override fun getId(domain: Conversation): String {
        return domain.id
    }

    override suspend fun findOrCreateConversation(
        userId: String,
        otherUserId: String
    ): Conversation = suspendTransaction {
        val existing = findConversationBetweenUsers(userId, otherUserId)
        return@suspendTransaction existing ?: createConversation(userId, otherUserId)
    }

    override suspend fun getUserConversations(userId: String): List<Conversation> = suspendTransaction {
        ParticipantsEntity.find {
            (ParticipantsTable.user).eq(userId) and ParticipantsTable.isActive
        }.map { it.conversation }
            .sortedByDescending { it.updatedAt }
            .map { it.toDomain() }
    }

    private suspend fun findConversationBetweenUsers(userId: String, otherUserId: String): Conversation? =
        suspendTransaction {
            val userConversations =
                ParticipantsEntity.find { ParticipantsTable.user.eq(userId) }
                    .map { it.conversation.id.value }

            ParticipantsEntity.find {
                (ParticipantsTable.user.eq(otherUserId)) and
                        (ParticipantsTable.conversation inList userConversations) and ParticipantsTable.isActive
            }.firstOrNull()?.conversation?.toDomain()
        }

    private suspend fun createConversation(userId: String, otherUserId: String): Conversation = suspendTransaction {
        val conversation = ConversationEntity.new {
            createdAt = LocalDateTime.now()
            updatedAt = LocalDateTime.now()
        }

        // Add participants
        listOf(userId, otherUserId).forEach { useId ->
            ParticipantsEntity.new {
                this.conversation = conversation
                this.user = UserEntity[userId]
                joinedAt = LocalDateTime.now()
                isActive = true
            }
        }
        return@suspendTransaction conversation.toDomain()
    }
}

class ParticipantRepositoryImp(private val participantMapper: ParticipantMapper) :
    CrudRepositoryImpl<ParticipantsEntity, Participant>(
        ParticipantsEntity, Participant::class
    ) {
    override fun ParticipantsEntity.toDomain(): Participant {
        return participantMapper.toModel(this)
    }

    override fun Participant.toEntity(entity: ParticipantsEntity) {
        participantMapper.toEntity(this, entity)
    }

    override fun getId(domain: Participant): String {
        return domain.id
    }
}

class MessageRepositoryImpl(private val messageMapper: MessageMapper) : CrudRepositoryImpl<MessageEntity, Message>(
    MessageEntity, Message::class
), MessageRepository {
    override fun MessageEntity.toDomain(): Message {
        return messageMapper.toModel(this)
    }

    override fun Message.toEntity(entity: MessageEntity) {
        messageMapper.toEntity(this, entity)
    }

    override fun getId(domain: Message): String {
        return domain.id
    }

    override suspend fun sendMessage(
        conversationId: String,
        senderId: String,
        text: String,
        type: MessageType
    ): Message = suspendTransaction {
        val message = MessageEntity.new {
            this.conversation = ConversationEntity[conversationId]
            this.sender = UserEntity[senderId]
            this.messageText = text
            this.messageType = type
        }

        val participants = ParticipantsEntity.find {
            (ParticipantsTable.conversation.eq(conversationId)) and ParticipantsTable.isActive
        }

        participants.forEach { participant ->
            if (participant.user.id.value != senderId) {
                MessageStatusEntity.new {
                    this.message = message
                    this.user = participant.user
                    this.status = MessageStatus.SENT
                    this.updatedAt = LocalDateTime.now()
                }
            }
        }

        ConversationEntity.findByIdAndUpdate(conversationId) { conv ->
            conv.updatedAt = LocalDateTime.now()
            conv.lastMessage = message
        }

        return@suspendTransaction message.toDomain()
    }

    override suspend fun getConversationMessages(conversationId: String, currentUserId: String): List<Message> =
        suspendTransaction {
            MessageEntity.find { MessagesTable.conversation.eq(conversationId) }
                .orderBy(MessagesTable.createdAt to SortOrder.ASC).map {
                    val message = it.toDomain()
                    val userStatus = MessageStatusEntity.find {
                        (MessageStatusTable.message.eq(it.id.value)) and
                                (MessageStatusTable.user.eq(currentUserId))
                    }.firstOrNull()

                    if (userStatus != null) {
                        message.copy(status = userStatus.status)
                    } else {
                        message
                    }
                }
        }

    override suspend fun markMessageAsRead(messageId: String, userId: String): Unit = suspendTransaction {
        MessageStatusEntity.findSingleByAndUpdate(
            (MessageStatusTable.message.eq(messageId)) and (MessageStatusTable.user.eq(userId))
        ) { msg ->
            msg.status = MessageStatus.READ
            msg.updatedAt = LocalDateTime.now()
        }
    }

    override suspend fun getUnreadCount(conversationId: String, userId: String): Int = suspendTransaction {
        val messageIds = MessageStatusEntity.find {
            (MessagesTable.conversation.eq(conversationId))
        }.map { it.id.value }

        if (messageIds.isEmpty()) return@suspendTransaction 0

        MessageStatusEntity.find {
            (MessageStatusTable.user.eq(userId)) and (MessageStatusTable.message inList messageIds) and (MessageStatusTable.status.eq(
                MessageStatus.READ
            ))
        }.count().toInt()
    }

    override suspend fun markConversationMessagesAsRead(conversationId: String, userId: String) = suspendTransaction {
        val messages = MessageEntity.find { MessagesTable.conversation.eq(conversationId) }

        messages.forEach { message ->
            val status = MessageStatusEntity.find {
                (MessageStatusTable.message.eq(message.id.value)) and (MessageStatusTable.user.eq(userId))
            }.firstOrNull()

            if (status != null) {
                status.status = MessageStatus.READ
                status.updatedAt = LocalDateTime.now()
            } else {
                MessageStatusEntity.new {
                    this.message = message
                    this.user = UserEntity[userId]
                    this.status = MessageStatus.READ
                    this.updatedAt = LocalDateTime.now()
                }
            }
        }
    }
}

