package org.example.data.repo

import org.example.data.db.entities.DeviceTokenEntity
import org.example.data.db.entities.NotificationEntity
import org.example.data.db.tables.DeviceTokenTable
import org.example.data.mappers.DeviceTokenMapper
import org.example.data.mappers.NotificationMapper
import org.example.domain.models.system.Notification
import org.example.domain.models.system.DeviceToken
import org.example.domain.repo.DeviceTokenRepository
import org.example.utils.suspendTransaction

class NotificationRepositoryImpl(private val notificationMapper: NotificationMapper) :
    CrudRepositoryImpl<NotificationEntity, Notification>(
        NotificationEntity,
        Notification::class
    ) {
    override fun NotificationEntity.toDomain(): Notification {
        return notificationMapper.toModel(this)
    }

    override fun Notification.toEntity(entity: NotificationEntity) {
        notificationMapper.toEntity(this, entity)
    }

    override fun getId(domain: Notification): String = domain.id
}

class DeviceTokenRepositoryImpl(private val deviceTokenMapper: DeviceTokenMapper) : CrudRepositoryImpl<DeviceTokenEntity, DeviceToken>(
    DeviceTokenEntity, DeviceToken::class
), DeviceTokenRepository {
    override fun DeviceTokenEntity.toDomain(): DeviceToken {
        return deviceTokenMapper.toModel(this)
    }

    override fun DeviceToken.toEntity(entity: DeviceTokenEntity) {
        deviceTokenMapper.toEntity(this, entity)
    }

    override fun getId(domain: DeviceToken): String {
        return domain.id
    }

    override suspend fun findUserDeviceTokens(userId: String): List<DeviceToken> = suspendTransaction {
        DeviceTokenEntity.find { DeviceTokenTable.user.eq(userId) }
            .map { it.toDomain() }
    }
}