package org.example.services

import org.example.data.repo.ConversationRepository
import org.example.data.repo.MessageRepository
import org.example.domain.models.support.Conversation
import org.example.domain.models.support.Message
import org.example.domain.models.support.MessageType

class ChatService(
    private val conversationRepo: ConversationRepository,
    private val messageRepo: MessageRepository
) {
    suspend fun startChat(userId: String, otherUserId: String): Conversation {
        return conversationRepo.findOrCreateConversation(userId, otherUserId)
    }

    suspend fun sendMessage(conversationId: String, senderId: String, text: String, type: MessageType): Message {
        return messageRepo.sendMessage(conversationId, senderId, text, type)
    }

    suspend fun getChatHistory(conversationId: String, currentUserId: String): List<Message> {
        return messageRepo.getConversationMessages(conversationId, currentUserId)
    }

    suspend fun getUserChats(userId: String): List<Conversation> {
        return conversationRepo.getUserConversations(userId)
    }
}