package org.example.domain.repo

import org.example.domain.models.system.Notification
import org.example.domain.models.system.DeviceToken
import org.example.domain.models.system.DispatchResult

interface NotificationRepository {
    suspend fun send(notification: Notification, email: String? = null, phoneNumber: String? = null): DispatchResult
    suspend fun markAsRead(notificationId: String, userId: String)
    suspend fun getUnreadCount(userId: String): Long
}

interface DeviceTokenRepository : CrudRepository<DeviceToken, String> {
    suspend fun findUserDeviceTokens(userId: String): List<DeviceToken>
    suspend fun deactivateToken(tokenId: String)
}

