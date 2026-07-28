package org.example.data.cache

import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands
import kotlinx.serialization.KSerializer
import org.example.data.mappers.UserMapper
import org.example.data.repo.CacheConfig
import org.example.data.repo.CrudCache
import org.example.data.repo.UserRepository
import org.example.domain.models.identity.TokenResponse
import org.example.domain.models.identity.User
import org.koin.core.annotation.Single
import java.net.InetAddress

@OptIn(ExperimentalLettuceCoroutinesApi::class)
@Single
class UserCache(
    redis: RedisCoroutinesCommands<String, String>,
    val userRepository: UserRepository,
    userMapper: UserMapper,
    serializer: KSerializer<User>,
    config: CacheConfig
): CrudCache<User, String>(
    redis = redis,
    delegate = userRepository,
    getId = { user -> userMapper.getId(user) as String },
    serializer = serializer,
    config = config
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