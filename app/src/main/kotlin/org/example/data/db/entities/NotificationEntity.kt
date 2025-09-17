package org.example.data.db.entities

import org.example.data.db.tables.NotificationTable
import org.example.data.db.config.CustomEntity
import org.example.data.db.config.CustomEntityClass
import org.jetbrains.exposed.v1.core.dao.id.EntityID

class NotificationEntity(id: EntityID<String>): CustomEntity(id, NotificationTable) {
    companion object : CustomEntityClass<NotificationEntity>(NotificationTable)

    var title by NotificationTable.title
    var message by NotificationTable.message
    var recipient by NotificationTable.recipient
    var channel by NotificationTable.channel
}