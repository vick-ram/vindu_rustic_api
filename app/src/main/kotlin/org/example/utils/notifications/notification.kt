package org.example.utils.notifications

import kotlinx.coroutines.delay
import org.example.domain.models.AppNotification
import org.example.domain.models.NotificationChannel
import org.example.domain.repo.NotificationRepository

class NotificationDispatcher(
    private val services: Map<NotificationChannel, NotificationRepository>,
    private val maxRetries: Int = 3,
    private val delay: Long = 1000
) {
    suspend fun dispatch(appNotification: AppNotification) {
        retry(maxRetries, delay) {
            services[appNotification.channel]?.send(appNotification)
        }
    }

    suspend fun dispatchToMultiple(
        channels: List<NotificationChannel>,
        base: AppNotification
    ): Map<NotificationChannel, Result<Unit>> {
        return channels.associateWith { channel ->
            runCatching {
                val notif = base.copy(channel = channel)
                dispatch(notif)
            }
        }
    }

    private suspend fun <T> retry(maxRetries: Int, delay: Long, block: suspend () -> T): T {
        var lastException: Throwable? = null
        repeat(maxRetries) { attempt ->
            try {
                return block()
            } catch (e: Exception) {
                lastException = e
                if (attempt < maxRetries - 1) {
                    delay(delay * (attempt + 1)) // Exponential backoff
                }
            }
        }
        throw lastException ?: RuntimeException("All retry attempts failed")
    }
}

