package org.example.data.mappers

import org.example.data.db.entities.UserEntity
import org.example.domain.models.identity.User
import org.example.domain.repo.EntityMapper

object UserMapper : EntityMapper<UserEntity, User, String> {
    override fun toModel(entity: UserEntity): User {
        return User(
            id = entity.id.value,
            email = entity.email,
            password = entity.password,
            firstName = entity.firstName,
            lastName = entity.lastName,
            phoneNumber = entity.phoneNumber,
            avatarUrl = entity.avatarUrl,
            emailVerified = entity.emailVerified,
            phoneVerified = entity.phoneVerified,
            status = entity.status,
            lastLoginAt = entity.lastLoginAt,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            deletedAt = entity.deletedAt
        )
    }

    override fun toEntity(
        model: User,
        entity: UserEntity
    ): UserEntity {
        val userAvatar = "/resources/images/profile.jpg"
        entity.email = model.email
        entity.password = model.password
        entity.firstName = model.firstName
        entity.lastName = model.lastName
        entity.phoneNumber = model.phoneNumber
        entity.avatarUrl = model.avatarUrl
        entity.emailVerified = model.emailVerified
        entity.phoneVerified = model.phoneVerified
        entity.status = model.status
        entity.lastLoginAt = model.lastLoginAt
        entity.deletedAt = model.deletedAt
        return entity
    }
}
