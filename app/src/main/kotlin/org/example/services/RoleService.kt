package org.example.services

import org.example.domain.models.identity.Role

class RoleService(private val roleRepository: RoleRepository) {
    suspend fun createRole(role: Role): Role? {
        return roleRepository.create(role)
    }

    suspend fun getRole(id: String): Role? {
        return roleRepository.read(id)
    }

    suspend fun getRoles(offset: Int = 0, limit: Int =  10, queryParams: Map<String, String>): List<Role> {
        return roleRepository.readAll(offset, limit, queryParams)
    }
    
    suspend fun updateRole(id: String, role: Role): Role? {
        return roleRepository.update(id, role)
    }

    suspend fun searchRole(query: String, offset: Int, limit: Int): List<Role> {
        return roleRepository.searchRole(query, offset, limit)
    }

    suspend fun deleteRole(id: String): Boolean {
        return roleRepository.delete(id)
    }
}