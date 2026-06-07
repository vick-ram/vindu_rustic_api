package org.example.data.db.entities

import org.example.data.db.config.CustomEntity
import org.example.data.db.config.CustomEntityClass
import org.example.data.db.tables.Sessions
import org.jetbrains.exposed.v1.core.dao.id.EntityID

class SessionEntity(id: EntityID<String>): CustomEntity(id, Sessions) {
    companion object: CustomEntityClass<SessionEntity>(Sessions)

    var userId by Sessions.userId
    var refreshTokenHash by Sessions.refreshTokenHash
    var ipAddress by Sessions.ipAddress
    var userAgent by Sessions.userAgent
    var expiresAt by Sessions.expiresAt
}