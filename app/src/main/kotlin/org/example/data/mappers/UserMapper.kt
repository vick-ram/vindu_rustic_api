package org.example.data.mappers

import org.example.data.db.entities.UserEntity
import org.example.domain.models.User
import org.example.domain.repo.EntityMapper

object UserMapper: EntityMapper<UserEntity, User, String> {
    override fun toModel(entity: UserEntity): User {
        return User(
            id = entity.id.value,
            name = entity.name,
            email = entity.email,
            password = entity.password,
            active = entity.active
        )
    }

    override fun toEntity(
        model: User,
        entity: UserEntity
    ): UserEntity {
        entity.name = model.name
        entity.email = model.email
        entity.password = model.password
        entity.active = model.active

        return entity
    }
}