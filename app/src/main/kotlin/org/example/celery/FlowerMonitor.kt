package org.example.celery

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

class FlowerMonitor(private val eventBus: EventBus) {
    private val workers = ConcurrentHashMap<String, WorkerMetrics>()
    private val totalTasks = AtomicLong(0)
    private val failedTasks = AtomicLong(0)

    suspend fun start() {
        eventBus.events.collect { event ->
            when (event.type) {
                EventType.WORKER_ONLINE -> {
                    workers[event.workerName] = WorkerMetrics(
                        activeTasks = 0,
                        processedTasks = 0,
                        failedTasks = 0,
                        uptime = 0,
                        loadAverage = 0.0
                    )
                }

                EventType.WORKER_HEARTBEAT -> {
                    event.metrics?.let { workers[event.workerName] = it }
                }

                EventType.TASK_SUCCEEDED -> totalTasks.incrementAndGet()
                EventType.TASK_FAILED -> {
                    totalTasks.incrementAndGet()
                    failedTasks.incrementAndGet()
                }
                else -> {}
            }
        }
    }

    fun getStats(): MonitorStats {
        return MonitorStats(
            activeWorkers = workers.size,
            totalTasks = totalTasks.get(),
            failedTasks = failedTasks.get(),
            workers = workers.toMap()
        )
    }
}

data class MonitorStats(
    val activeWorkers: Long,
    val totalTasks: Long,
    val failedTasks: Long,
    val workers: Map<String, WorkerMetrics>
)