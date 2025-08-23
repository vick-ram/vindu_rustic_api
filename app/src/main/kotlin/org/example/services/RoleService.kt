package org.example.services

import org.example.domain.models.Role
import org.example.domain.repo.CrudRepository

class RoleService(private val crudRepository: CrudRepository<Role, String>) {
    suspend fun createRole(role: Role): Role? {
        return crudRepository.create(role)
    }

    suspend fun getRole(id: String): Role? {
        return crudRepository.read(id)
    }

    suspend fun getRoles(offset: Int = 0, limit: Int =  10, queryParams: Map<String, String>): List<Role> {
        return crudRepository.readAll(offset, limit, queryParams)
    }
    
    suspend fun updateRole(id: String, role: Role): Role? {
        return crudRepository.update(id, role)
    }

    suspend fun deleteRole(id: String): Boolean {
        return crudRepository.delete(id)
    }
}