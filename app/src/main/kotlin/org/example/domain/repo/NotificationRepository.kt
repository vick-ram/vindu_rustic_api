package org.example.domain.repo

import org.example.domain.models.system.Notification
import org.example.domain.models.system.DeviceToken

interface NotificationRepository {
    suspend fun send(notification: Notification)
}

interface DeviceTokenRepository : CrudRepository<DeviceToken, String> {
    suspend fun findUserDeviceTokens(userId: String): List<DeviceToken>
}

