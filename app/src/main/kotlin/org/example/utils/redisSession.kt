package org.example.utils

import io.ktor.server.sessions.SessionStorage
import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands

@OptIn(ExperimentalLettuceCoroutinesApi::class)
class RedisSessionStorage(private val redis: RedisCoroutinesCommands<String, String>) : SessionStorage {

    override suspend fun invalidate(id: String) {
        redis.del(id)
    }

    override suspend fun read(id: String): String {
        val value = redis.get(id)
        return value ?: ""
    }

    override suspend fun write(id: String, value: String) {
        redis.set(id, value)
    }
}