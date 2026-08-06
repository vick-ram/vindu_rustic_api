package org.example.data.cache

import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands
import kotlinx.serialization.KSerializer
import org.example.data.mappers.UserMapper
import org.example.data.repo.CacheConfig
import org.example.data.repo.CrudCache
import org.example.data.repo.UserRepository
import org.example.di.Component
import org.example.di.Inject
import org.example.domain.models.identity.User

@OptIn(ExperimentalLettuceCoroutinesApi::class)
@Component
class UserCache @Inject constructor(
    redis: RedisCoroutinesCommands<String, String>,
    val userRepository: UserRepository,
    userMapper: UserMapper,
): CrudCache<User, String>(
    redis = redis,
    delegate = userRepository,
    getId = { user -> userMapper.getId(user) as String },
    serializer = User.serializer(),
    config = object : CacheConfig {
        override val cacheName: String = "user_cache"
        override val ttl: Long = 3600L
    }
) {

    suspend fun readByEmail(email: String): User? {
        return userRepository.findByEmail(email)
    }

    suspend fun searchUsers(query: String, offset: Int, limit: Int): List<User> {
        return userRepository.searchUsers(query, offset, limit)
    }

    suspend fun updatePassword(userId: String, newPassword: String) {
        userRepository.updatePassword(userId, newPassword)
    }

    suspend fun update2FAStatus(userId: String, enabled: Boolean) {
        userRepository.update2FAStatus(userId, enabled)
    }
}