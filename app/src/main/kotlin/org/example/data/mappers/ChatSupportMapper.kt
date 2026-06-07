package org.example.data.mappers

import kotlinx.datetime.LocalDateTime
import org.example.data.db.entities.ConversationEntity
import org.example.data.db.entities.MessageEntity
import org.example.data.db.entities.ParticipantsEntity
import org.example.data.db.entities.UserEntity
import org.example.domain.models.support.Conversation
import org.example.domain.models.support.Message
import org.example.domain.models.support.Participant
import org.example.domain.repo.EntityMapper
import org.example.utils.now

object ConversationMapper : EntityMapper<ConversationEntity, Conversation, String> {
    override fun toModel(entity: ConversationEntity): Conversation {
        return Conversation(
            id = entity.id.value,
            lastMessageId = entity.lastMessage?.id?.value,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }

    override fun toEntity(
        model: Conversation,
        entity: ConversationEntity
    ): ConversationEntity {
        return entity.apply {
            this.lastMessage = model.lastMessageId?.let { MessageEntity[it] }
            this.createdAt = LocalDateTime.now()
            this.updatedAt = LocalDateTime.now()
        }
    }
}

object ParticipantMapper : EntityMapper<ParticipantsEntity, Participant, String> {
    override fun toModel(entity: ParticipantsEntity): Participant {
        return Participant(
            id = entity.id.value,
            conversationId = entity.conversation.id.value,
            userId = entity.user.id.value,
            joinedAt = entity.joinedAt,
            leftAt = entity.leftAt,
            isActive = entity.isActive
        )
    }

    override fun toEntity(
        model: Participant,
        entity: ParticipantsEntity
    ): ParticipantsEntity {
        return entity.apply {
            this.conversation = ConversationEntity[model.conversationId]
            this.user = UserEntity[model.userId]
            this.joinedAt = model.joinedAt
            this.leftAt = model.leftAt
            this.isActive = model.isActive
        }
    }
}

object MessageMapper : EntityMapper<MessageEntity, Message, String> {
    override fun toModel(entity: MessageEntity): Message {
        return Message(
            id = entity.id.value,
            conversationId = entity.conversation.id.value,
            senderId = entity.sender.id.value,
            messageText = entity.messageText,
            messageType = entity.messageType,
            parentMessageId = entity.parentMessage?.id?.value,
            createdAt = entity.createdAt,
            fileUrl = entity.fileUrl,
            fileName = entity.fileName,
            fileSize = entity.fileSize
        )
    }

    override fun toEntity(
        model: Message,
        entity: MessageEntity
    ): MessageEntity {
        return entity.apply {
            this.conversation = ConversationEntity[model.conversationId]
            this.sender = UserEntity[model.senderId]
            this.messageText = model.messageText
            this.messageType = model.messageType
            this.parentMessage = model.parentMessageId?.let { MessageEntity[it] }
            this.createdAt = LocalDateTime.now()
            this.fileUrl = model.fileUrl
            this.fileName = model.fileName
            this.fileSize = model.fileSize
        }
    }
}