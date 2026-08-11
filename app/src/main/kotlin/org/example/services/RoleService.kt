package org.example.services

import kotlinx.coroutines.flow.toList
import org.example.data.repo.RoleRepository
import org.example.data.repo.UserRoleRepository
import org.example.di.Component
import org.example.di.Inject
import org.example.domain.models.identity.Role
import org.example.domain.models.identity.UserRole
import org.example.exceptions.NotFoundException

@Component
class RoleService @Inject constructor(
    private val roleRepository: RoleRepository,
    private val userRoleRepository: UserRoleRepository
) {
    suspend fun createRole(role: Role): Role {
        return roleRepository.create(role)
    }

    suspend fun getRoleById(id: String): Role? {
        return roleRepository.read(id)
    }

    suspend fun getRoleByName(name: String) = roleRepository.findByName(name) ?: throw NotFoundException("Role with name '$name' not found")

    suspend fun getRoles(offset: Int = 0, limit: Int =  10, queryParams: Map<String, String>): List<Role> {
        return roleRepository.readAll(offset, limit, queryParams).toList()
    }

    suspend fun searchRoles(query: String, offset: Int = 0, limit: Int = 20): List<Role> {
        if (query.isBlank()) return roleRepository.readAll(offset, limit).toList()
        return roleRepository.search(
            query, offset.coerceAtLeast(0), limit.coerceIn(1, 100)
        )
    }
    
    suspend fun updateRole(id: String, role: Role): Role? {
        return roleRepository.update(id, role)
    }

    suspend fun deleteRole(id: String): Boolean {
        getRoleById(id)
        userRoleRepository.findByRoleId(id).forEach {userRole ->
            userRoleRepository.removeRole(userRole.userId, userRole.roleId)
        }
        return roleRepository.delete(id)
    }

    suspend fun getRolesForUser(userId: String): List<Role> {
        return roleRepository.findByUserId(userId)
    }

    suspend fun assignRoleToUser(userId: String, roleId: String): UserRole {
        getRoleById(roleId)
        return userRoleRepository.assignRole(userId, roleId)
    }

    suspend fun assignRoleByNameToUser(userId: String, roleName: String): UserRole {
        val role = getRoleByName(roleName)
        return assignRoleToUser(userId, role.id)
    }

    suspend fun removeRoleFromUser(userId: String, roleId: String): Boolean {
        return userRoleRepository.removeRole(userId = userId, roleId = roleId)
    }

    suspend fun hasRole(userId: String, roleId: String): Boolean {
        if (userId.isBlank() || roleId.isBlank()) return false
        return userRoleRepository.hasRole(userId = userId, roleId = roleId)
    }

    suspend fun hasRoleByName(userId: String, roleName: String): Boolean {
        val role = roleRepository.findByName(roleName) ?: return false
        return userRoleRepository.hasRole(userId = userId, roleId = role.id)
    }

    suspend fun syncUserRoles(userId: String, roleIds: List<String>): List<UserRole> {
        // Validate that all specified roles exist before syncing
        val distinctRoleIds = roleIds.distinct()
        distinctRoleIds.forEach { roleId ->
            getRoleById(roleId)
        }
        return userRoleRepository.syncRoles(userId = userId, roleIds = distinctRoleIds)
    }

    suspend fun bulkAssignRoles(userId: String, roleIds: List<String>): List<UserRole> {

        val distinctRoleIds = roleIds.distinct()
        distinctRoleIds.forEach { roleId ->
            getRoleById(roleId)
        }

        return userRoleRepository.bulkAssignRoles(userId = userId, roleIds = distinctRoleIds)
    }

    suspend fun removeAllRolesFromUser(userId: String): Int {
        return userRoleRepository.removeAllRoles(userId)
    }
}