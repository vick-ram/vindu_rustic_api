package org.example.data.cache

import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands
import org.example.data.mappers.ConversationMapper
import org.example.data.mappers.MessageMapper
import org.example.data.repo.CacheConfig
import org.example.data.repo.ConversationRepository
import org.example.data.repo.CrudCache
import org.example.data.repo.MessageRepository
import org.example.domain.models.support.Conversation
import org.example.domain.models.support.Message
import org.koin.core.annotation.Single

@OptIn(ExperimentalLettuceCoroutinesApi::class)
@Single
class ConversationCache(
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


@OptIn(ExperimentalLettuceCoroutinesApi::class)
@Single
class MessageCache(
    redis: RedisCoroutinesCommands<String, String>,
    private val messageRepository: MessageRepository,
    messageMapper: MessageMapper
): CrudCache<Message, String>(
    redis = redis,
    delegate = messageRepository,
    getId = {message -> messageMapper.getId(message) as String},
    serializer = Message.serializer(),
    config = object : CacheConfig {
        override val cacheName: String = "MessageCache"
        override val ttl: Long = 3600L
    }
) {}