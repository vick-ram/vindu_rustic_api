package org.example.utils

import io.ktor.server.sessions.SessionStorage
import io.lettuce.core.ExperimentalLettuceCoroutinesApi

@OptIn(ExperimentalLettuceCoroutinesApi::class)
class RedisSessionStorage() : SessionStorage {

    override suspend fun invalidate(id: String) {
        RedisService.commands.del(id)
    }

    override suspend fun read(id: String): String {
        val value = RedisService.commands.get(id)
        return value ?: ""
    }

    override suspend fun write(id: String, value: String) {
        RedisService.commands.set(id, value)
    }
}