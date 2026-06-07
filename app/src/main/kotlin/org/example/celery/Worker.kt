package org.example.celery

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
    private val semaphore = Semaphore(concurrency)
    private val activeTasks = ConcurrentHashMap<String, Job>()

    //    private val pool = WorkerPool(concurrency)
    @Volatile
    private var isRunning = false

    suspend fun start() {
        isRunning = true
        logger.info("Worker $name starting with concurrency $concurrency")
        logger.info("Listening on queues: ${queues.joinToString()}")

        queues.forEach { queue ->
            broker.consume(queue).collect { record ->
                semaphore.withPermit {
                    executeTask(record)
                }
            }
        }

        while (isRunning) {
            delay(1000.milliseconds)
        }
    }

    private fun executeTask(record: BrokerRecord<TaskMessage>) {
        val taskMessage = record.payload

        val job = scope.launch {
            try {
                logger.debug("Executing task: ${taskMessage.id} (${taskMessage.taskName})")

                // update state STARTED
                updateTaskState(taskMessage.id, TaskState.STARTED)

                // Get task implementation from registry
                val taskImpl = taskRegistry.getTask(taskMessage.taskName)
                    ?: throw IllegalArgumentException("Unknown task: ${taskMessage.taskName}")

                // Execute with time limits
                val result = withTimeoutOrNull((taskMessage.timeLimit?.times(1000) ?: Long.MAX_VALUE).milliseconds) {
                    taskImpl.run(
                        args = taskMessage.args.toTypedArray(),
                        kwargs = taskMessage.kwargs
                    )
                }

                if (result == null && taskMessage.timeLimit != null) {
                    throw TimeoutException("Task exceeded time limit of ${taskMessage.timeLimit}s")
                }

                val res = mapOf("result" to JsonPrimitive(result.toString()))

                val taskResult = TaskResult(
                    taskId = taskMessage.id,
                    state = TaskState.SUCCESS,
                    result = result as? JsonObject ?: res.toJsonObject(),
                    dateDone = Instant.now(),
                    worker = name
                )

                backend?.storeResult(taskMessage.id, taskResult)
            } catch (_: CancellationException) {
                updateTaskState(taskMessage.id, TaskState.REVOKED)
            } catch (e: Exception) {
                logger.error("Task ${taskMessage.id} failed", e)

                // Handle retry logic
                if (taskMessage.retries < taskMessage.maxRetries) {
                    val retryMessage = taskMessage.copy(retries = taskMessage.retries + 1)
                    broker.publish(retryMessage, taskMessage.queue, taskMessage.priority)

                    updateTaskState(taskMessage.id, TaskState.RETRY)
                } else {
                    val failedResult = TaskResult(
                        taskId = taskMessage.id,
                        state = TaskState.FAILURE,
                        traceback = e.stackTraceToString(),
                        dateDone = Instant.now(),
                        worker = name
                    )

                    backend?.storeResult(taskMessage.id, failedResult)
                }
                broker.reject(record.deliveryTag, false)
            } finally {
                activeTasks.remove(taskMessage.id)
            }
        }

        activeTasks[taskMessage.id] = job
    }

    private suspend fun updateTaskState(taskId: String, state: TaskState) {
        val result = TaskResult(
            taskId = taskId,
            state = state,
            worker = name
        )
        backend?.storeResult(taskId, result)
    }

    suspend fun stop() {
        isRunning = false
        scope.cancel()
        broker.close()
        logger.info("Worker $name stopped")
    }
}