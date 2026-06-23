package org.example.data.db.entities

import org.example.data.db.config.CustomEntity
import org.example.data.db.config.CustomEntityClass
import org.example.data.db.tables.DeviceTokenTable
import org.example.data.db.tables.Notifications
import org.jetbrains.exposed.v1.core.dao.id.EntityID

class NotificationEntity(id: EntityID<String>): CustomEntity(id, Notifications) {
    companion object : CustomEntityClass<NotificationEntity>(Notifications)

    var userId by Notifications.userId
    var type by Notifications.type
    var title by Notifications.title
    var body by Notifications.body
    var actionUrl by Notifications.actionUrl
    var referenceType by Notifications.referenceType
    var referenceId by Notifications.referenceId
    var isRead by Notifications.isRead
    var readAt by Notifications.readAt
}

class DeviceTokenEntity(id: EntityID<String>) : CustomEntity(id, DeviceTokenTable) {
    companion object : CustomEntityClass<DeviceTokenEntity>(DeviceTokenTable)

    var user by UserEntity referencedOn DeviceTokenTable.user
    var token by DeviceTokenTable.token
    var platform by DeviceTokenTable.platform
    var isActive by DeviceTokenTable.isActive
}