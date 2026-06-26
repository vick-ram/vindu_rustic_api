package org.example.di

import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands
import org.example.data.cache.ConversationCache
import org.example.data.cache.MessageCache
import org.example.data.cache.UserCache
import org.example.data.mappers.ConversationMapper
import org.example.data.mappers.MessageMapper
import org.example.data.mappers.UserMapper
import org.example.data.repo.CacheConfig
import org.example.data.repo.ConversationRepository
import org.example.data.repo.CrudCache
import org.example.data.repo.MessageRepository
import org.example.data.repo.UserRepository
import org.example.domain.models.identity.User
import org.example.domain.models.support.Conversation
import org.example.domain.models.support.Message
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@OptIn(ExperimentalLettuceCoroutinesApi::class)
@Module
class CacheModule {

    @Single
    fun provideUserCache(
        redis: RedisCoroutinesCommands<String, String>,
        userRepository: UserRepository,
        userMapper: UserMapper
    ): UserCache {
        return UserCache(
            redis = redis,
            userRepository = userRepository,
            userMapper = userMapper,
            serializer = User.serializer(),
            config = object : CacheConfig {
                override val cacheName: String
                    get() = "users"
                override val ttl: Long
                    get() = 3600L
            }
        )
    }

    @Single
    fun provideConversationCache(
        redis: RedisCoroutinesCommands<String, String>,
        conversationRepo: ConversationRepository,
        conversationMapper: ConversationMapper
    ) : ConversationCache {
        return ConversationCache(
            redis = redis,
            conversationRepository = conversationRepo,
            conversationMapper = conversationMapper
        )
    }

    @Single
    fun provideMessageCache(
        redis: RedisCoroutinesCommands<String, String>,
        messageRepository: MessageRepository,
        messageMapper: MessageMapper
    ) : MessageCache {
        return MessageCache(
            redis = redis,
            messageRepository = messageRepository,
            messageMapper = messageMapper
        )
    }
}