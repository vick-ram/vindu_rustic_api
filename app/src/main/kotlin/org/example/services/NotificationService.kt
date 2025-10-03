package org.example.services

import org.example.domain.models.AppNotification
import org.example.domain.models.DeviceToken
import org.example.domain.repo.CrudRepository
import org.example.domain.repo.DeviceTokenRepository

class NotificationService(private val crudRepo: CrudRepository<AppNotification, String>) {
    suspend fun save(appNotification: AppNotification): AppNotification {
        return crudRepo.create(appNotification)
    }

    suspend fun getAll(offset: Int, limit: Int, queryParams: Map<String, String>): List<AppNotification> {
        return crudRepo.readAll(offset, limit, queryParams)
    }
}

class DeviceTokenService(private val deviceTokenRepository: DeviceTokenRepository) {
    suspend fun saveDeviceToken(deviceToken: DeviceToken): DeviceToken {
        return deviceTokenRepository.create(deviceToken)
    }

    suspend fun getDeviceTokens(offset: Int, limit: Int, queryParams: Map<String, String>): List<DeviceToken> {
        return deviceTokenRepository.readAll(offset, limit, queryParams)
    }

    suspend fun getUserDeviceTokens(userId: String): List<DeviceToken> {
        return deviceTokenRepository.findUserDeviceTokens(userId)
    }

    suspend fun deleteDeviceToken(id: String): Boolean {
        return deviceTokenRepository.delete(id)
    }
}