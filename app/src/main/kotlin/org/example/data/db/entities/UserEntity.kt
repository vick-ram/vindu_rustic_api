package org.example.data.db.entities

import org.example.data.db.tables.Users
import org.example.data.db.config.CustomEntity
import org.example.data.db.config.CustomEntityClass
import org.example.data.db.tables.Addresses
import org.example.data.db.tables.Sessions
import org.example.data.db.tables.UserRoles
import org.jetbrains.exposed.v1.core.dao.id.EntityID

class UserEntity(id: EntityID<String>): CustomEntity(id, Users) {
    companion object : CustomEntityClass<UserEntity>(Users)

    var email by Users.email
    var password by Users.password
    var firstName by Users.firstName
    var lastName by Users.lastName
    var phoneNumber by Users.phoneNumber
    var avatarUrl by Users.avatarUrl
    var emailVerified by Users.emailVerified
    var phoneVerified by Users.phoneVerified
    var status by Users.status
    var lastLoginAt by Users.lastLoginAt
    var deletedAt by Users.deletedAt

    val roles by UserEntity via UserRoles
    val addresses by AddressEntity referrersOn Addresses.userId
    val sessions by SessionEntity referrersOn Sessions.userId
    var tsv by Users.tsv

}