package org.example

// Permission.kt
data class Permission(
    val id: Int,
    val name: String,
    val description: String
)

data class UserPermission(
    val userId: Int,
    val permissionId: Int
)

data class PermissionCheckRequest(
    val userId: Int,
    val permissionName: String
)

// PermissionService.kt
interface PermissionService {
    suspend fun prepopulatePermissions()
    suspend fun hasPermission(userId: Int, permissionName: String): Boolean
    suspend fun grantPermission(userId: Int, permissionName: String): Boolean
    suspend fun revokePermission(userId: Int, permissionName: String): Boolean
    suspend fun getUserPermissions(userId: Int): List<Permission>
}

//class PermissionServiceImpl(private val database: Database) : PermissionService {
//
//    override suspend fun prepopulatePermissions() {
//        val defaultPermissions = listOf(
//            Permission(1, "user.read", "Read user information"),
//            Permission(2, "user.write", "Modify user information"),
//            Permission(3, "user.delete", "Delete users"),
//            Permission(4, "admin.access", "Access admin panel"),
//            Permission(5, "content.create", "Create content"),
//            Permission(6, "content.edit", "Edit content"),
//            Permission(7, "content.delete", "Delete content"),
//            Permission(8, "api.access", "Access API endpoints")
//        )
//
//        database.transaction {
//            defaultPermissions.forEach { permission ->
//                // Insert if not exists
//                val exists = Permissions.select { Permissions.name eq permission.name }.count() > 0
//                if (!exists) {
//                    Permissions.insert {
//                        it[name] = permission.name
//                        it[description] = permission.description
//                    }
//                }
//            }
//        }
//    }
//
//    override suspend fun hasPermission(userId: Int, permissionName: String): Boolean {
//        return database.transaction {
//            (Permissions innerJoin UserPermissions)
//                .select {
//                    (UserPermissions.userId eq userId) and
//                            (Permissions.name eq permissionName)
//                }
//                .count() > 0
//        }
//    }
//
//    override suspend fun grantPermission(userId: Int, permissionName: String): Boolean {
//        return database.transaction {
//            val permission = Permissions.select { Permissions.name eq permissionName }.singleOrNull()
//            permission?.get(Permissions.id)?.let { permissionId ->
//                val exists = UserPermissions.select {
//                    (UserPermissions.userId eq userId) and
//                            (UserPermissions.permissionId eq permissionId)
//                }.count() > 0
//
//                if (!exists) {
//                    UserPermissions.insert {
//                        it[UserPermissions.userId] = userId
//                        it[UserPermissions.permissionId] = permissionId
//                    }
//                    true
//                } else {
//                    true // Already granted
//                }
//            } ?: false
//        }
//    }
//
//    override suspend fun revokePermission(userId: Int, permissionName: String): Boolean {
//        return database.transaction {
//            val permission = Permissions.select { Permissions.name eq permissionName }.singleOrNull()
//            permission?.get(Permissions.id)?.let { permissionId ->
//                UserPermissions.deleteWhere {
//                    (UserPermissions.userId eq userId) and
//                            (UserPermissions.permissionId eq permissionId)
//                } > 0
//            } ?: false
//        }
//    }
//
//    override suspend fun getUserPermissions(userId: Int): List<Permission> {
//        return database.transaction {
//            (Permissions innerJoin UserPermissions)
//                .select { UserPermissions.userId eq userId }
//                .map {
//                    Permission(
//                        id = it[Permissions.id],
//                        name = it[Permissions.name],
//                        description = it[Permissions.description]
//                    )
//                }
//        }
//    }
//}