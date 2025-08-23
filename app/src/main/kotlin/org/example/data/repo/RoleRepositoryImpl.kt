package org.example.data.repo

import org.example.data.db.entities.RoleEntity
import org.example.data.mappers.RoleMapper
import org.example.domain.models.Role

class RoleRepositoryImpl(private val roleMapper: RoleMapper): CrudRepositoryImpl<RoleEntity, Role>(RoleEntity) {
    override fun RoleEntity.toDomain(): Role = roleMapper.toModel(this)

    override fun Role.toEntity(entity: RoleEntity) {
        roleMapper.toEntity(this, entity)
    }
}