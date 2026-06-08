package org.example.celery

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import java.time.Instant
import java.util.UUID

data class TaskMessage(
    val id: String = UUID.randomUUID().toString(),
    val taskName: String,
    val args: List<JsonElement> = emptyList(),
    val kwargs: Map<String, JsonElement> = emptyMap(),
    val retries: Int = 0,
    val maxRetries: Int = 3,
    val eta: Long? = null, // estimated time of arrival
    val expires: Long? = null,
    val priority: Int = 0,
    val queue: String = "default"
)

enum class TaskState {
    PENDING,
    STARTED,
    SUCCESS,
    FAILURE,
    RETRY,
    REVOKED
}

data class TaskResult(
    val taskId: String,
    val state: TaskState,
    val result: JsonElement? = null,
    val traceback: String? = null,
    val dateDone: Instant? = null,
)

abstract class CeleryTask(
    val name: String,
    val maxRetries: Int = 3,
    val defaultRetryDelay: Long = 60
) {
    abstract suspend fun run(vararg args: Any?, kwargs: Map<String, Any?>) : Any?
}

fun JsonElement.toAny(): Any? = when (this) {
    is JsonNull -> null
    is JsonPrimitive -> when {
        isBoolean -> asBoolean
        isNumber -> asNumber
        isString -> asString
        else -> asString
    }

    is JsonObject -> entrySet().associate { (key, value) ->
        key to value.toAny()
    }

    is JsonArray -> map { it.toAny() }
    else -> null
}

fun Any?.toJsonElement(): JsonElement = when (this) {
    is String -> JsonPrimitive(this)
    is Number -> JsonPrimitive(this)
    is Boolean -> JsonPrimitive(this)
    null -> JsonNull.INSTANCE
    else -> JsonPrimitive(this.toString())
}