package org.example.celery

import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

class CeleryApp(
    val name: String = "celery-app",
    private val broker: MessageBroker,
    private val backend: ResultBackend? = null
) {
    private val taskRegistry = TaskRegistry()
    private val workers = ConcurrentLinkedQueue<Worker>()
    private var scheduler: CeleryBeatScheduler? = null
    private val periodicTasks = ConcurrentHashMap<String, PeriodicTask>()

    fun registerTask(task: CeleryTask) {
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
        val message = TaskMessage(
            taskName = taskName,
            args = args.map { it.toJsonElement() },
            kwargs = kwargs.mapValues { it.value.toJsonElement() },
            queue = queue,
            priority = priority,
            eta = eta?.let { (it.epochSecond + (countdown ?: 0L)) },
            expires = expires
        )

        broker.publish(message, queue, priority)
        return message
    }

    suspend fun startWorkers(
        count: Int = 1,
        queues: List<String> = listOf("default"),
        concurrency: Int = 4
    ) {
        repeat(count) { index ->
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

    fun schedulePeriodicTask(name: String, task: TaskSignature, schedule: Schedule) {
        periodicTasks[name] = PeriodicTask(name, task, schedule)
    }

    suspend fun startScheduler() {
        scheduler = CeleryBeatScheduler(periodicTasks.toMap()) { taskSignature ->
            sendTask(
                taskName = taskSignature.taskName,
                args = taskSignature.args.map { it.toAny() },
                kwargs = taskSignature.kwargs.mapValues { it.value.toAny() },
                queue = taskSignature.queue,
                priority = taskSignature.priority
            )
        }
        scheduler?.start()
    }

    suspend fun getResult(taskId: String): TaskResult? = backend?.getResult(taskId)

    suspend fun shutdown() {
        scheduler?.stop()
        workers.forEach { it.stop() }
        broker.close()
        backend?.close()
    }
}