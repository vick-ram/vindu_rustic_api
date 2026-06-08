package org.example.celery

import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.milliseconds

class Worker(
    val name: String = "worker-${nextId.getAndIncrement()}",
    private val queues: List<String> = listOf("default"),
    private val concurrency: Int = 4,
    private val broker: MessageBroker,
    private val backend: ResultBackend? = null,
    private val taskRegistry: TaskRegistry
) {
    companion object {
        private val nextId = AtomicInteger(1)
    }

    private val logger = LoggerFactory.getLogger("Worker[$name]")
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val activeJobs = ConcurrentHashMap<String, Job>()

    suspend fun start() {
        logger.info("Starting worker: $name")

        queues.forEach { queue ->
            repeat(concurrency) {
                scope.launch {
                    broker.consume(queue).collect { record ->
                        val task = record.payload
                        val celeryTask = taskRegistry.getTask(task.taskName)

                        if (celeryTask == null) {
                            logger.error("Unknown task: ${task.taskName}")
                            broker.reject(record.deliveryTag, requeue = false)
                            return@collect
                        }

                        try {
                            backend?.storeResult(
                                task.id,
                                TaskResult(task.id, TaskState.STARTED)
                            )

                            val args = task.args.map { it.toAny() }.toTypedArray()
                            val kwargs = task.kwargs.mapValues { it.value.toAny() }

                            val result = celeryTask.run(*args, kwargs = kwargs)

                            backend?.storeResult(
                                task.id,
                                TaskResult(
                                    taskId = task.id,
                                    state = TaskState.SUCCESS,
                                    result = result.toJsonElement()
                                )
                            )

                            broker.acknowledge(record.deliveryTag)
                        } catch (e: Exception) {
                            logger.error("Task ${task.taskName} failed", e)
                            handleFailure(task, celeryTask, e, record)
                        }
                    }
                }
            }
        }
    }

    private suspend fun handleFailure(
        task: TaskMessage,
        celeryTask: CeleryTask,
        exception: Exception,
        record: BrokerRecord<TaskMessage>
    ) {
        if (task.retries < task.maxRetries) {
            val retryTask = task.copy(retries = task.retries + 1)
            broker.publish(retryTask, task.queue, task.priority)
            broker.acknowledge(record.deliveryTag)
        } else {
            backend?.storeResult(
                task.id,
                TaskResult(
                    taskId = task.id,
                    state = TaskState.FAILURE,
                    traceback = exception.stackTraceToString()
                )
            )
            broker.reject(record.deliveryTag, requeue = false)
        }
    }

    fun stop() {
        scope.cancel()
    }
}