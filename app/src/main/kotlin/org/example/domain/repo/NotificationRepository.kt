package org.example.domain.repo

import org.example.domain.models.Notification

interface NotificationRepository {
    suspend fun send(notification: Notification)
}