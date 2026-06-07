package org.example.data.mappers

import org.example.data.db.entities.RoleEntity
import org.example.domain.models.identity.Role
import org.example.domain.repo.EntityMapper

object RoleMapper : EntityMapper<RoleEntity, Role, String> {
    override fun toModel(entity: RoleEntity): Role {
        return Role(
            id = entity.id.value,
            name = entity.name,
            description = entity.description
        )
    }

    override fun toEntity(model: Role, entity: RoleEntity): RoleEntity {
        entity.name = model.name
        entity.description = model.description
        return entity
    }
}