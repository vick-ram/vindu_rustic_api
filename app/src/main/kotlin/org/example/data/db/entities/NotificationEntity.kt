package org.example.data.db.entities

import org.example.data.db.config.CustomEntity
import org.example.data.db.config.CustomEntityClass
import org.example.data.db.tables.DeviceTokenTable
import org.example.data.db.tables.NotificationTable
import org.jetbrains.exposed.v1.core.dao.id.EntityID

class NotificationEntity(id: EntityID<String>): CustomEntity(id, NotificationTable) {
    companion object : CustomEntityClass<NotificationEntity>(NotificationTable)

    var title by NotificationTable.title
    var message by NotificationTable.message
    var topic by NotificationTable.topic
    var channel by NotificationTable.channel
    var imageUrl by NotificationTable.imageUrl
    var metadata by NotificationTable.metadata
}

class DeviceTokenEntity(id: EntityID<String>) : CustomEntity(id, DeviceTokenTable) {
    companion object : CustomEntityClass<DeviceTokenEntity>(DeviceTokenTable)

    var user by UserEntity referencedOn DeviceTokenTable.user
    var token by DeviceTokenTable.token
    var platform by DeviceTokenTable.platform
}