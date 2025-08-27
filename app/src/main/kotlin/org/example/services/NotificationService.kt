package org.example.services

import org.example.domain.models.Notification
import org.example.domain.repo.CrudRepository

class NotificationService(private val crudRepo: CrudRepository<Notification, String>) {
    suspend fun save(notification: Notification): Notification? {
        return crudRepo.create(notification)
    }

    suspend fun getAll(offset: Int, limit: Int, queryParams: Map<String, String>): List<Notification> {
        return crudRepo.readAll(offset, limit, queryParams)
    }
}