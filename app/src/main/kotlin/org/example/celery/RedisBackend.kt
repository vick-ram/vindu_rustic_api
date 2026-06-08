package org.example.celery

import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands
import org.example.utils.Json

@OptIn(ExperimentalLettuceCoroutinesApi::class)
class RedisBackend(
    private val redis: RedisCoroutinesCommands<String, String>,
    private val json: Json,
    private val prefix: String = "celery"
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

    override suspend fun close() {
        // Redis connection managed externally
    }

    private fun taskKey(taskId: String) = "$prefix:result:$taskId"
}