package org.example.domain.repo

import org.example.domain.models.support.Conversation
import org.example.domain.models.support.Message
import org.example.domain.models.support.MessageType

interface ConversationRepository {
    suspend fun findOrCreateConversation(userId: String, otherUserId: String): Conversation
    suspend fun getUserConversations(userId: String): List<Conversation>
}

interface MessageRepository {
    suspend fun sendMessage(conversationId: String, senderId: String, text: String, type: MessageType = MessageType.TEXT): Message
    suspend fun getConversationMessages(conversationId: String, currentUserId: String): List<Message>
    suspend fun markMessageAsRead(messageId: String, userId: String)
    suspend fun markConversationMessagesAsRead(conversationId: String, userId: String)
    suspend fun getUnreadCount(conversationId: String, userId: String): Int
}