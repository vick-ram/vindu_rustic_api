package org.example.celery

import com.google.gson.Gson
import com.google.gson.JsonObject
import java.time.Instant
import java.util.UUID

data class TaskMessage(
    val id: String = UUID.randomUUID().toString(),
    val taskName: String,
    val args: List<JsonObject> = emptyList(),
    val kwargs: Map<String, JsonObject> = emptyMap(),
    val retries: Int = 0,
    val maxRetries: Int = 3,
    val eta: Long? = null, // estimated time of arrival
    val expires: Long? = null,
    val priority: Int = 0,
    val queue: String = "default",
    val routingKey: String = "default",
    val deliveryTag: String? = null,
    val origin: String? = null,
    val timeLimit: Long? = null,
    val softTimeLimit: Long? = null,
)

enum class TaskState {
    PENDING,
    RECEIVED,
    STARTED,
    SUCCESS,
    FAILURE,
    RETRY,
    REVOKED,
    REJECTED,
    IGNORED
}

data class TaskResult(
    val taskId: String,
    val state: TaskState,
    val result: JsonObject? = null,
    val traceback: String? = null,
    val dateDone: Instant? = null,
    val worker: String? = null
)

abstract class CeleryTask {
    abstract val name: String
    abstract val maxRetries: Int
    abstract val defaultRetryDelay: Long
    abstract val timeLimit: Long?
    abstract val softTimeLimit: Long?

    abstract suspend fun run(vararg args: Any?, kwargs: Map<String, Any>?) : Any?

    suspend fun applyAsync(
        args: List<Any?> = emptyList(),
        kwargs: Map<String, Any?> = emptyMap(),
        countdown: Long? = null,
        eta: Instant? = null,
        expires: Long? = null,
        priority: Int = 0,
        queue: String = "default",
        routingKey: String = "default",
    ): TaskMessage {
        throw NotImplementedError("Use CeleryApp.sendTask()")
    }

    suspend fun retry(
        exc: Exception? = null,
        countdown: Long? = null,
        maxRetries: Int? = null,
    ) {
        throw NotImplementedError("Use worker.retry()")
    }
}

fun Map<String, Any?>.toJsonObject(gson: Gson = Gson()): JsonObject {
    val jsonObject = JsonObject()
    for ((key, value) in this) {
        jsonObject.add(key, gson.toJsonTree(value))
    }
    return jsonObject
}