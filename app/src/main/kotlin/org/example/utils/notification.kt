package org.example.utils

import org.example.domain.models.Notification
import org.example.domain.models.NotificationChannel
import org.example.domain.repo.NotificationRepository

class NotificationDispatcher(
    private val services: Map<NotificationChannel, NotificationRepository>
) {
    suspend fun dispatch(notification: Notification) {
        services[notification.channel]?.send(notification)
            ?: throw IllegalArgumentException("No service for ${notification.channel}")
    }

    suspend fun dispatchToMultiple(channels: List<NotificationChannel>, base: Notification) {
        channels.forEach { channel ->
            val notif = base.copy(channel = channel)
            dispatch(notif)
        }
    }
}

