package org.example.services

import org.example.data.cache.ConversationCache
import org.example.di.Component
import org.example.di.Inject
import org.example.domain.models.support.Conversation

@Component
class ConversationService @Inject constructor(private val cache: ConversationCache) {
    suspend fun createConversation(conversation: Conversation): Conversation = cache.create(conversation)
    suspend fun getConversation(id: String): Conversation? = cache.read(id)
    suspend fun getConversations(offset: Int = 0, limit: Int = 20, queryParams: Map<String, String>? = null): List<Conversation> = cache.readAll(offset, limit, queryParams)
    suspend fun updateConversation(id: String, conversation: Conversation): Conversation? = cache.update(id, conversation)
    suspend fun deleteConversation(id: String): Boolean = cache.delete(id)
}
