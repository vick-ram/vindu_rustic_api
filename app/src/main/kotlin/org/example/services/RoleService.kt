package org.example.services

import kotlinx.coroutines.flow.toList
import org.example.data.repo.RoleRepository
import org.example.di.Component
import org.example.di.Inject
import org.example.domain.models.identity.Role

@Component
class RoleService @Inject constructor(private val roleRepository: RoleRepository) {
    suspend fun createRole(role: Role): Role {
        return roleRepository.create(role)
    }

    suspend fun getRole(id: String): Role? {
        return roleRepository.read(id)
    }

    suspend fun getRoles(offset: Int = 0, limit: Int =  10, queryParams: Map<String, String>): List<Role> {
        return roleRepository.readAll(offset, limit, queryParams).toList()
    }
    
    suspend fun updateRole(id: String, role: Role): Role? {
        return roleRepository.update(id, role)
    }

    suspend fun deleteRole(id: String): Boolean {
        return roleRepository.delete(id)
    }
}