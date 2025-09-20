package org.example.domain.repo

import org.example.domain.models.Role

interface RoleRepository: CrudRepository<Role, String> {
    suspend fun searchRole(query: String, offset: Int, limit: Int): List<Role>
}

class CachedRole(
    private val delegate: RoleRepository,
    private val cache: CrudRepository<Role, String>
) : RoleRepository {
    override suspend fun searchRole(
        query: String,
        offset: Int,
        limit: Int
    ): List<Role> {
        return delegate.searchRole(query, offset, limit)
    }

    override suspend fun create(entity: Role): Role {
        return cache.create(entity)
    }

    override suspend fun read(id: String): Role? {
        return cache.read(id)
    }

    override suspend fun readAll(
        offset: Int,
        limit: Int,
        queryParams: Map<String, String>?
    ): List<Role> {
        return cache.readAll(offset, limit, queryParams)
    }

    override suspend fun update(
        id: String,
        entity: Role
    ): Role? {
        return cache.update(id, entity)
    }

    override suspend fun delete(id: String): Boolean {
        return cache.delete(id)
    }
}