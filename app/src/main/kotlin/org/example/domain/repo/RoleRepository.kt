package org.example.domain.repo

import org.example.domain.models.Role

interface RoleRepository: CrudRepository<Role, String> {
    suspend fun searchRole(query: String, offset: Int, limit: Int): List<Role>
}