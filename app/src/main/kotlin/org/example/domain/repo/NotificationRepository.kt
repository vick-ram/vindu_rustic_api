package org.example.domain.repo

import org.example.domain.models.AppNotification
import org.example.domain.models.DeviceToken

interface NotificationRepository {
    suspend fun send(appNotification: AppNotification)
}

interface DeviceTokenRepository : CrudRepository<DeviceToken, String> {
    suspend fun findUserDeviceTokens(userId: String): List<DeviceToken>
}

