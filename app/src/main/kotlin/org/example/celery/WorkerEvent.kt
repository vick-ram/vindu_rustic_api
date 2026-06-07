package org.example.celery

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.time.Instant

data class WorkerEvent(
    val type: EventType,
    val workerName: String,
    val timestamp: Instant = Instant.now(),
    val task: TaskMessage? = null,
    val result: TaskResult? = null,
    val metrics: WorkerMetrics? = null
)

enum class EventType {
    WORKER_ONLINE,
    WORKER_OFFLINE,
    WORKER_HEARTBEAT,
    TASK_RECEIVED,
    TASK_STARTED,
    TASK_SUCCEEDED,
    TASK_FAILED,
    TASK_RETIRED,
    TASK_REVOKED
}

data class WorkerMetrics(
    val activeTasks: Int,
    val processedTasks: Long,
    val failedTasks: Long,
    val uptime: Long,
    val loadAverage: Double
)

class EventBus {
    private val _events = MutableSharedFlow<WorkerEvent>(replay = 1000)
    val events = _events.asSharedFlow()

    suspend fun emit(event: WorkerEvent) {
        _events.emit(event)
    }
}
