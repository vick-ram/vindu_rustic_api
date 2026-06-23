package org.example.config.security

import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands

@OptIn(ExperimentalLettuceCoroutinesApi::class)
class TokenService(private val redis: RedisCoroutinesCommands<String, String>) {
    suspend fun blacklistToken(token: String, expirationMs: Long) {
        redis.setex("blacklist:$token", expirationMs / 1000,"blacklisted")
    }

    suspend fun isBlacklisted(token: String): Boolean {
        return (redis.exists("blacklist:$token") ?: 0L) > 0L
    }

    suspend fun storeRefreshToken(userId: String, token: String, expirationMs: Long) {
        redis.setex("refresh:$userId:$token", expirationMs / 1000, "valid")
    }

    suspend fun invalidateAllUserTokens(userId: String) {
        redis.keys("refresh:$userId:*").collect { key ->
            redis.del(key)
        }
    }
}