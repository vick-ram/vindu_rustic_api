package org.example.celery

import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands
import org.example.utils.Json

@OptIn(ExperimentalLettuceCoroutinesApi::class)
class RedisBackend(
    private val prefix: String = "celery",
    private val redis: RedisCoroutinesCommands<String, String>,
    private val json: Json
): ResultBackend {
    override suspend fun storeResult(
        taskId: String,
        result: TaskResult,
        expirySeconds: Long
    ) {
        val key = taskKey(taskId)
        redis.apply {
            set(key, json.encodeToString(result))
            expire(key, expirySeconds)
        }
    }

    override suspend fun getResult(taskId: String): TaskResult? {
        val key = taskKey(taskId)
        return redis.get(key)?.let { json.decodeFromString(it) }
    }

    override suspend fun forgetResult(taskId: String) {
        redis.del(taskKey(taskId))
    }

    override suspend fun close() {
        TODO("Not yet implemented")
    }

    private fun taskKey(taskId: String) = "$prefix:result:$taskId"
}