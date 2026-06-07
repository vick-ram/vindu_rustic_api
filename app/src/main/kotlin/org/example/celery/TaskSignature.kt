package org.example.celery

import com.google.gson.JsonObject
import java.time.Instant

data class TaskSignature(
    val taskName: String,
    val args: List<JsonObject> = emptyList(),
    val kwargs: Map<String, JsonObject> = emptyMap(),
    val options: TaskOptions = TaskOptions(),
    var immutable: Boolean = false,
    var subtasks: List<TaskSignature> = emptyList()
) {
    fun set(
        queue: String? = null,
        priority: Int? = null,
        countdown: Long? = null,
        eta: Instant? = null,
        expires: Long? = null
    ): TaskSignature {
        return this.copy(
            options = options.copy(
                queue = queue ?: options.queue,
                priority = priority ?: options.priority,
                countdown = countdown ?: options.countdown,
                eta = eta ?: options.eta,
                expires = expires ?: options.expires
            )
        )
    }

    infix fun chain(next: TaskSignature): TaskSignature {
        return this.copy(subtasks = subtasks + next)
    }

    companion object {
        fun group(vararg tasks: TaskSignature): List<TaskSignature> = tasks.toList()
        fun chord(
            header: List<TaskSignature>,
            callback: TaskSignature
        ): TaskSignature {
            return callback.copy(subtasks = header)
        }
    }
}

data class TaskOptions(
    val queue: String = "default",
    val priority: Int = 0,
    val countdown: Long? = null,
    val eta: Instant? = null,
    val expires: Long? = null,
    val retry: Boolean = true,
    val retryPolicy: RetryPolicy = RetryPolicy()
)

data class RetryPolicy(
    val maxRetries: Int = 3,
    val intervalStart: Double = 0.0,
    val intervalStep: Double = 0.2,
    val intervalMax: Double = 0.2,
)