package org.example.celery

import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import kotlin.time.Duration.Companion.milliseconds

class CeleryBeatScheduler(
    private val schedule: Map<String, PeriodicTask>,
    private val onSchedule: suspend (String, TaskSignature) -> Unit
) {
    private val logger = LoggerFactory.getLogger(CeleryBeatScheduler::class.java)
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val lastRunTimes = mutableMapOf<String, Instant>()

    suspend fun start() {
        logger.info("Celery Beat scheduler starting")

        while (currentCoroutineContext().isActive) {
            val now = Instant.now()

            schedule.forEach { (name, task) ->
                val schedule = task.schedule
                val lastRun = lastRunTimes[name]

                val shouldRun = when (schedule) {
                    is Schedule.Timedelta -> {
                        lastRun == null ||
                                Duration.between(lastRun, now).seconds >= schedule.seconds
                    }
                    is Schedule.Crontab -> {
                        schedule.crontab.isDue(now)
                    }
                }

                if (shouldRun){
                    scope.launch {
                        try {
                            logger.debug("Running periodic task: $name")
                            onSchedule(name, task.task)
                            lastRunTimes[name] = now
                        } catch (e: Exception) {
                            logger.error("Failed to schedule periodic task: $name", e)
                        }
                    }
                }
            }

            delay(1000.milliseconds)
        }
    }

    fun stop() {
        scope.cancel()
    }
}