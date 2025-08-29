package org.example.data.repo

import org.example.data.db.entities.RoleEntity
import org.example.data.db.tables.RoleTable
import org.example.data.mappers.RoleMapper
import org.example.domain.models.Role
import org.example.domain.repo.RoleRepository
import org.example.utils.customMatch
import org.example.utils.suspendTransaction

class RoleRepositoryImpl(private val roleMapper: RoleMapper) :
    CrudRepositoryImpl<RoleEntity, Role>(RoleEntity, Role::class), RoleRepository {
    override fun RoleEntity.toDomain(): Role {
        return roleMapper.toModel(this)
    }

    override suspend fun searchRole(
        query: String,
        offset: Int,
        limit: Int
    ): List<Role> = suspendTransaction {
        RoleEntity.find { RoleTable.tsv.customMatch(query) }
            .offset(offset.toLong())
            .limit(limit)
            .map { it.toDomain() }
    }

    override fun Role.toEntity(entity: RoleEntity) {
        roleMapper.toEntity(this, entity)
    }
}