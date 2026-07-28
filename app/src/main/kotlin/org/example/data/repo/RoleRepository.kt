package org.example.data.repo

import io.r2dbc.spi.Connection
import io.r2dbc.spi.ConnectionFactory
import io.r2dbc.spi.RowMetadata
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrElse
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.serialization.Serializable
import org.example.data.mappers.RoleMapper
import org.example.data.mappers.UserRoleMapper
import org.example.di.Component
import org.example.di.Inject
import org.example.domain.models.identity.Role
import org.example.domain.models.identity.UserRole
import org.koin.core.annotation.Single

@Component
class RoleRepository @Inject constructor(
    connectionFactory: ConnectionFactory,
    roleMapper: RoleMapper
) : CrudRepository<Role, String>(
    connectionFactory = connectionFactory,
    tableName = "roles",
    mapper = roleMapper
) {
    override val generatedColumns = listOf("id")

    // Find role by name
    suspend fun findByName(name: String): Role? {
        val sql = "SELECT * FROM $tableName WHERE LOWER(name) = LOWER(:name)"

        return connectionFactory.useConnection {
            createNamedStatement(sql, mapOf("name" to name))
                .execute()
                .awaitSingle()
                .map(rowMapper)
                .awaitFirstOrNull()
        }
    }

    // Search roles
    suspend fun search(
        query: String,
        offset: Int = 0,
        limit: Int = 20
    ): List<Role> {
        val sql = """
            SELECT * FROM $tableName 
            WHERE name ILIKE :query 
               OR description ILIKE :query 
            ORDER BY name ASC 
            LIMIT :limit OFFSET :offset
        """.trimIndent()

        return executeQuery(sql, mapOf(
            "query" to "%$query%",
            "limit" to limit,
            "offset" to offset.toLong()
        ))
    }

    // Get roles for a user
    suspend fun findByUserId(userId: String): List<Role> {
        val sql = """
            SELECT r.* 
            FROM $tableName r
            INNER JOIN user_roles ur ON r.id = ur.role_id
            WHERE ur.user_id = :userId
            ORDER BY r.name ASC
        """.trimIndent()

        return executeQuery(sql, mapOf("userId" to userId))
    }

    // Check if role exists
    suspend fun existsByName(name: String): Boolean {
        val sql = "SELECT COUNT(*) as count FROM $tableName WHERE LOWER(name) = LOWER(:name)"

        return connectionFactory.useConnection {
            createNamedStatement(sql, mapOf("name" to name))
                .execute()
                .awaitSingle()
                .map { row, _ -> row.get("count", Long::class.java)!! > 0 }
                .awaitFirstOrNull() ?: false
        }
    }

    // Get role with user count
    suspend fun getRolesWithUserCount(): List<RoleWithUserCount> {
        val sql = """
            SELECT r.*, COUNT(ur.user_id) as user_count
            FROM $tableName r
            LEFT JOIN user_roles ur ON r.id = ur.role_id
            GROUP BY r.id
            ORDER BY r.name ASC
        """.trimIndent()

        return connectionFactory.useConnection {
            createStatement(sql)
                .execute()
                .awaitSingle()
                .map { row, metadata ->
                    RoleWithUserCount(
                        role = rowMapper.apply(row, metadata),
                        userCount = row.get("user_count", Long::class.java)!!.toInt()
                    )
                }
                .asFlow()
                .toList()
        }
    }
}

class UserRoleRepository(
    connectionFactory: ConnectionFactory,
    userRoleMapper: UserRoleMapper
) : CrudRepository<UserRole, String>(
    connectionFactory = connectionFactory,
    tableName = "user_roles",
    idColumn = "user_id",  // Note: This table has a composite key
    mapper = userRoleMapper
) {
    override val generatedColumns = listOf("created_at")

    // Since UserRole has a composite key, we need custom implementations

    override suspend fun create(model: UserRole, connection: Connection?): UserRole {
        val sql = """
            INSERT INTO $tableName (user_id, role_id)
            VALUES (:userId, :roleId)
            ON CONFLICT (user_id, role_id) DO NOTHING
            RETURNING *
        """.trimIndent()

        val params = mapOf("userId" to model.userId, "roleId" to model.roleId)

        val conn = connection ?: return connectionFactory.withTransaction {
            it.createNamedStatement(sql, params)
                .execute()
                .awaitSingle()
                .map(rowMapper)
                .awaitFirstOrElse {
                    model // Return the original model if already exists
                }
        }
        return conn.createNamedStatement(sql, params)
            .execute()
            .awaitSingle()
            .map(rowMapper)
            .awaitFirstOrElse {
                model // Return the original model if already exists
            }
    }

    // Assign role to user
    suspend fun assignRole(userId: String, roleId: String): UserRole {
        return create(UserRole(userId = userId, roleId = roleId))
    }

    // Remove role from user
    suspend fun removeRole(userId: String, roleId: String): Boolean {
        val sql = """
            DELETE FROM $tableName 
            WHERE user_id = :userId AND role_id = :roleId
        """.trimIndent()

        return connectionFactory.withTransaction { connection ->
            connection.createNamedStatement(sql, mapOf("userId" to userId, "roleId" to roleId))
                .execute()
                .awaitSingle()
                .rowsUpdated
                .awaitSingle() > 0
        }
    }

    // Get roles for a user
    suspend fun findByUserId(userId: String): List<UserRole> {
        val sql = """
            SELECT * FROM $tableName 
            WHERE user_id = :userId
        """.trimIndent()

        return executeQuery(sql, mapOf("userId" to userId))
    }

    // Get users with a specific role
    suspend fun findByRoleId(roleId: String): List<UserRole> {
        val sql = """
            SELECT * FROM $tableName 
            WHERE role_id = :roleId
        """.trimIndent()

        return executeQuery(sql, mapOf("roleId" to roleId))
    }

    // Check if user has role
    suspend fun hasRole(userId: String, roleId: String): Boolean {
        val sql = """
            SELECT COUNT(*) as count 
            FROM $tableName 
            WHERE user_id = :userId AND role_id = :roleId
        """.trimIndent()

        return connectionFactory.useConnection {
            createNamedStatement(sql, mapOf("userId" to userId, "roleId" to roleId))
                .execute()
                .awaitSingle()
                .map { row, _ -> (row.get("count", Long::class.java) ?: 0L) > 0 }
                .awaitFirstOrNull() ?: false
        }
    }

    // Bulk assign roles to user
    suspend fun bulkAssignRoles(userId: String, roleIds: List<String>): List<UserRole> {
        if (roleIds.isEmpty()) return emptyList()

        val sql = """
        INSERT INTO $tableName (user_id, role_id)
        SELECT :userId, UNNEST(:roleIds)
        ON CONFLICT (user_id, role_id) DO NOTHING
        RETURNING *
    """.trimIndent()

        val params = mapOf(
            "userId" to userId,
            "roleIds" to roleIds.toTypedArray()
        )

        return connectionFactory.withTransaction { connection ->
            connection.createNamedStatement(sql, params)
                .execute()
                .awaitSingle()
                .map(rowMapper)
                .asFlow()
                .toList()
        }
    }

    // Remove all roles for a user
    suspend fun removeAllRoles(userId: String): Int {
        val sql = "DELETE FROM $tableName WHERE user_id = :userId"

        return connectionFactory.withTransaction { connection ->
            connection.createNamedStatement(sql, mapOf("userId" to userId))
                .execute()
                .awaitSingle()
                .rowsUpdated
                .awaitSingle()
                .toInt()
        }
    }

    // Sync user roles (remove all and assign new)
    suspend fun syncRoles(userId: String, roleIds: List<String>): List<UserRole> {
        return connectionFactory.withTransaction {
            removeAllRoles(userId)
            bulkAssignRoles(userId, roleIds)
        }
    }
}

@Serializable
data class RoleWithUserCount(
    val role: Role,
    val userCount: Int
)