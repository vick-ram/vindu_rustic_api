package org.example.utils.notifications

import io.celery.model.CeleryTask
import io.celery.model.TaskContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import org.example.domain.models.NotificationChannel
import org.example.domain.models.system.DispatchResult
import org.example.domain.models.system.MultiChannelResult
import org.example.domain.models.system.Notification
import org.example.domain.repo.NotificationRepository
import org.slf4j.LoggerFactory
import java.time.OffsetDateTime
import kotlin.math.pow
import kotlin.time.Duration.Companion.milliseconds

class DispatchNotificationTask(
    private val dispatcher: NotificationDispatcher,
    private val json: Json
) : CeleryTask<DispatchResult>(
    name = "dispatch_notification",
    maxRetries = 3,
    defaultRetryDelay = 60,
    serializer = DispatchResult.serializer()
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun run(context: TaskContext): DispatchResult {
        logger.info(
            "Processing dispatch task ${context.taskId} " +
                    "(attempt ${context.attempt})"
        )
        val notification = json.decodeFromJsonElement<Notification>(context.kwargs["notification"]!!)
        return dispatcher.dispatch(notification)
    }
}


class DispatchMultiChannelNotificationTask(
    private val dispatcher: NotificationDispatcher,
    private val json: Json
) : CeleryTask<MultiChannelResult>(
    name = "dispatch_multi_channel_notification",
    maxRetries = 2,
    defaultRetryDelay = 30,
    serializer = MultiChannelResult.serializer()
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun run(context: TaskContext): MultiChannelResult {
        logger.info(
            "Processing multi-channel dispatch task ${context.taskId}"
        )

        val notification = json.decodeFromJsonElement<Notification>(
            context.kwargs["notification"]
                ?: error("Missing required argument: notification")
        )

        val channels = json.decodeFromJsonElement<List<NotificationChannel>>(
            context.kwargs["channels"]
                ?: error("Missing required argument: channels")
        )
        logger.info(
            "Dispatching to ${channels.size} channels: ${channels.joinToString()}"
        )

        return dispatcher.dispatchToMultiple(channels, notification)
    }

    override fun onRetry(exc: Exception, retries: Int): Long {
        return  minOf(defaultRetryDelay * retries, 120)
    }
}

class NotificationDispatcher(
    private val services: Map<NotificationChannel, NotificationRepository>,
    private val maxRetries: Int = 3,
    private val baseDelay: Long = 1000,
    private val maxDelay: Long = 30000,
    private val backoffMultiplier: Double = 2.0
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun dispatch(notification: Notification): DispatchResult {
        logger.info("Dispatching notification ${notification.id} via ${notification.channel}")

        val service = services[notification.channel]
            ?: throw IllegalArgumentException("No service configured for channel: ${notification.channel}")

        return dispatchWithBackoff(
            maxRetries = maxRetries,
            baseDelay = baseDelay,
            operation = "dispatch_${notification.id}"
        ) {
            try {
                service.send(notification)
                logger.info("Successfully dispatched notification ${notification.id}")
                DispatchResult(
                    notificationId = notification.id,
                    channel = notification.channel,
                    success = true,
                    message = "Dispatched successfully"
                )
            } catch (e: Exception) {
                logger.error("Failed to dispatch notification ${notification.id}", e)
                throw NotificationDispatchException(
                    "Failed to dispatch notification ${notification.id}",
                    notification.channel,
                    e
                )
            }
        }
    }

    /**
     * Dispatch notification to multiple channels
     */
    suspend fun dispatchToMultiple(
        channels: List<NotificationChannel>,
        baseNotification: Notification
    ): MultiChannelResult = coroutineScope {
        logger.info("Dispatching notification ${baseNotification.id} to ${channels.size} channels")

        val results = channels.associateWith { channel ->
            async {
                try {
                    val notification = baseNotification.copy(
                        channel = channel,
                        updatedAt = OffsetDateTime.now()
                    )
                    dispatch(notification)
                } catch (e: Exception) {
                    logger.error("Failed to dispatch to channel $channel", e)
                    DispatchResult(
                        notificationId = baseNotification.id,
                        channel = channel,
                        success = false,
                        message = e.message
                    )
                }
            }
        }

        val dispatchResults = results.mapValues { it.value.await() }

        MultiChannelResult(
            notificationId = baseNotification.id,
            results = dispatchResults,
            successCount = dispatchResults.count { it.value.success },
            failureCount = dispatchResults.count { !it.value.success }
        )
    }

    private suspend fun <T> dispatchWithBackoff(
        maxRetries: Int,
        baseDelay: Long,
        operation: String,
        block: suspend () -> T
    ): T {
        var lastException: Throwable? = null

        for (attempt in 0..maxRetries) {
            try {
                return block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                lastException = e

                if (attempt < maxRetries) {
                    val delay = calculateBackoff(baseDelay, attempt, maxDelay)
                    logger.warn(
                        "Operation '$operation' failed (attempt ${attempt + 1}/${maxRetries + 1}), " +
                                "retrying in ${delay}ms",
                        e
                    )
                    delay(delay.milliseconds)

                }
            }
        }
        throw MaxRetriesExceededException(
            "Operation '$operation' failed after ${maxRetries + 1} attempts",
            lastException
        )
    }

    private fun calculateBackoff(baseDelay: Long, attempt: Int, maxDelay: Long): Long {
        return minOf(
            (baseDelay * backoffMultiplier.pow(attempt.toDouble())).toLong(),
            maxDelay
        )
    }
}

class NotificationDispatchException(
    message: String,
    val channel: NotificationChannel,
    cause: Throwable? = null
) : RuntimeException(message, cause)

class NoDeviceTokensException(userId: String) :
    RuntimeException("No device tokens found for user: $userId")

class MaxRetriesExceededException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)