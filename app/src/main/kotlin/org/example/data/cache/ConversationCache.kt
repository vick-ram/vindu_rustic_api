package org.example.data.cache

import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands
import org.example.data.mappers.ConversationMapper
import org.example.data.repo.CacheConfig
import org.example.data.repo.ConversationRepository
import org.example.data.repo.CrudCache
import org.example.di.Component
import org.example.di.Inject
import org.example.domain.models.support.Conversation

@OptIn(ExperimentalLettuceCoroutinesApi::class)
@Component
class ConversationCache @Inject constructor(
    redis: RedisCoroutinesCommands<String, String>,
    private val conversationRepository: ConversationRepository,
    conversationMapper: ConversationMapper
): CrudCache<Conversation, String>(
    redis = redis,
    delegate = conversationRepository,
    getId = {conversation -> conversationMapper.getId(conversation) as String},
    serializer = Conversation.serializer(),
    config = object : CacheConfig {
        override val cacheName: String = "ConversationCache"
        override val ttl: Long = 3600L
    }
) {
}
