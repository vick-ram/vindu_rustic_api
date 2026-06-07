package org.example.celery

import com.google.gson.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.time.Instant
import javax.annotation.Priority

class CeleryApp(
    val name: String = "celery-app",
    val broker: MessageBroker,
    val backend: ResultBackend? = null,
    val config: CeleryConfig = CeleryConfig()
) {
    private val taskRegistry = TaskRegistry()
    private val eventBus = EventBus()
    private val monitor = FlowerMonitor(eventBus)
    private var workers = mutableListOf<Worker>()
    private var scheduler: CeleryBeatScheduler? = null
    private val periodicTasks = mutableMapOf<String, PeriodicTask>()

    fun task(task: CeleryTask) {
        taskRegistry.register(task)
    }

    suspend fun sendTask(
        taskName: String,
        args: List<Any?> = emptyList(),
        kwargs: Map<String, Any?> = emptyMap(),
        queue: String = "default",
        priority: Int = 0,
        countdown: Long? = null,
        eta: Instant? = null,
        expires: Long? = null
    ): TaskMessage {
        val taskMessage = TaskMessage(
            taskName = taskName,
            args = args.map { it.toJsonObject() },
            kwargs = kwargs.mapValues { it.value.toJsonObject() },
            queue = queue,
            priority = priority,
            eta = eta?.toEpochMilli(),
            expires = expires
        )

        broker.publish(taskMessage, queue, priority)
        return taskMessage
    }

    suspend fun startWorkers(
        count: Int = 1,
        queues: List<String> = listOf("default"),
        concurrency: Int = 4
    ) {
        repeat(count) {index ->
            val worker = Worker(
                name = "${name}-worker-${index + 1}",
                queues = queues,
                concurrency = concurrency,
                broker = broker,
                backend = backend,
                taskRegistry = taskRegistry
            )
            workers.add(worker)
            worker.start()
        }
    }
    fun addPeriodicTak(name: String, task: TaskSignature, schedule: Schedule) {
        periodicTasks[name] = PeriodicTask(name, task, schedule)
    }

    suspend fun startBeatScheduler() {
        scheduler = CeleryBeatScheduler(periodicTasks) {_, taskSignature ->
            sendTask(
                taskName= taskSignature.taskName,
                args = taskSignature.args.map { it },
                kwargs = taskSignature.kwargs.mapValues { it.value},
                queue = taskSignature.options.queue,
                priority = taskSignature.options.priority
            )
        }
        scheduler?.start()
    }

    suspend fun getResult(taskId: String): TaskResult? = backend?.getResult(taskId)

    suspend fun revokeTask(taskId: String) {
        // implementation depends on broker capabilities
        // For Redis, you might send revoke message
    }

    fun getMonitor() = monitor

    suspend fun shutdown() {
        scheduler?.stop()
        workers.forEach { it.stop() }
        broker.close()
        backend?.close()
    }
}

private fun Any?.toJsonObject(): JsonObject {
    return mapOf("value" to JsonPrimitive(this.toString())).toJsonObject()
}


data class CeleryConfig(
    val taskSerialization: String = "json",
    val resultSerialization: String = "json",
    val acceptContent: List<String> = listOf("json"),
    val timezone: String = "UTC",
    val enableUtc: Boolean = true,
    val taskTrackStarted: Boolean = true,
    val taskSendSentEvent: Boolean = true,
    val workerMaxTasksPerChild: Int = 100,
    val workerPrefetchMultiplier: Int = 4,
    val taskAckLate: Boolean = true,
    val taskRejectOnWorkerLost: Boolean = true,
    val resultExpires: Long = 3600, // 1 hour
    val taskDefaultQueue: String = "default",
    val taskDefaultRoutingKey: String = "default",
    val taskDefaultPriority: Int = 0,
    val brokerConnectionRetry: Boolean = true,
    val brokerConnectionMaxRetries: Int = 100,
    val brokerConnectionTimeout: Int = 10
)