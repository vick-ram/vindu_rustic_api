package org.example.celery

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import java.time.Instant

data class TaskSignature(
    val taskName: String,
    val args: List<JsonElement> = emptyList(),
    val kwargs: Map<String, JsonElement> = emptyMap(),
    val queue: String = "default",
    val priority: Int = 0,
    val countdown: Long? = null,
    val eta: Instant? = null,
    val expires: Long? = null
)