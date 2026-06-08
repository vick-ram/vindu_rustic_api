package org.example.celery

import com.cronutils.model.CronType
import com.cronutils.model.definition.CronDefinitionBuilder
import com.cronutils.model.time.ExecutionTime
import com.cronutils.parser.CronParser
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.milliseconds

class CeleryBeatScheduler(
    private val periodicTasks: Map<String, PeriodicTask>,
    private val onSchedule: suspend (TaskSignature) -> Unit
) {
    private val logger = LoggerFactory.getLogger(CeleryBeatScheduler::class.java)
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val lastRunTimes = ConcurrentHashMap<String, Instant>()

    suspend fun start() {
        logger.info("Beat scheduler starting with {} tasks", periodicTasks.size)

        val cronParser = CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.QUARTZ)) // External library like cron-utils

        while (currentCoroutineContext().isActive) {
            val now = Instant.now()

            periodicTasks.forEach { (name, task) ->
                val shouldRun = when (val schedule = task.schedule) {
                    is Schedule.Timedelta -> {
                        val lastRun = lastRunTimes[name]
                        lastRun == null || Duration.between(lastRun, now).seconds >= schedule.seconds
                    }
                    is Schedule.Crontab -> {
                        val cron = cronParser.parse(schedule.expression)
                        val lastRun = lastRunTimes[name]?.atZone(ZoneId.systemDefault())
                        val nextRun = ExecutionTime.forCron(cron)
                            .nextExecution(lastRun ?: now.atZone(ZoneId.systemDefault()).minusSeconds(1))

                        nextRun.map { !it.toInstant().isAfter(now) }.orElse(false)
                    }
                }

                if (shouldRun) {
                    lastRunTimes[name] = now
                    scope.launch {
                        try {
                            logger.debug("Executing periodic task: $name")
                            onSchedule(task.task)
                        } catch (e: Exception) {
                            logger.error("Periodic task $name failed", e)
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
