package org.example.data.mappers

import org.example.data.db.entities.PermissionEntity
import org.example.data.db.entities.RoleEntity
import org.example.data.db.entities.UserEntity
import org.example.domain.models.Permission
import org.example.domain.models.Role
import org.example.domain.models.User
import org.example.domain.repo.EntityMapper
import org.example.utils.HashPassword

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
        entity.password = HashPassword.hashPassword(model.password)
        entity.active = model.active

        return entity
    }
}

object RoleMapper: EntityMapper<RoleEntity, Role, String> {
    override fun toModel(entity: RoleEntity): Role {
        return Role(
            id = entity.id.value,
            name = entity.name,
            description = entity.description,
        )
    }

    override fun toEntity(
        model: Role,
        entity: RoleEntity
    ): RoleEntity {
        entity.name = model.name
        entity.description = model.description

        return  entity
    }
}

object PermissionMapper: EntityMapper<PermissionEntity, Permission, String> {
    override fun toModel(entity: PermissionEntity): Permission {
        return Permission(
            id = entity.id.value,
            name = entity.name,
            description = entity.description
        )
    }

    override fun toEntity(
        model: Permission,
        entity: PermissionEntity
    ): PermissionEntity {
        entity.name = model.name
        entity.description = model.description

        return entity
    }
}