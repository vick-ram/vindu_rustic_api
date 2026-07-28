package org.example.data.repo

import io.r2dbc.spi.ConnectionFactory
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.serialization.Serializable
import org.example.data.mappers.DeviceTokenMapper
import org.example.di.Component
import org.example.di.Inject
import org.example.domain.models.system.DeviceToken
import java.time.OffsetDateTime

@Component
class DeviceTokenRepository @Inject constructor(
    connectionFactory: ConnectionFactory,
    deviceTokenMapper: DeviceTokenMapper
) : CrudRepository<DeviceToken, String>(
    connectionFactory = connectionFactory,
    tableName = "device_tokens",
    mapper = deviceTokenMapper
) {
    override val generatedColumns = listOf("id", "created_at")

    // Get tokens by user
    suspend fun findByUserId(userId: String): List<DeviceToken> {
        val sql = """
            SELECT * FROM $tableName 
            WHERE user_id = :userId 
              AND is_active = true 
            ORDER BY created_at DESC
        """.trimIndent()

        return executeQuery(sql, mapOf("userId" to userId))
    }

    // Find token by value
    suspend fun findByToken(token: String): DeviceToken? {
        val sql = """
            SELECT * FROM $tableName 
            WHERE token = :token 
            LIMIT 1
        """.trimIndent()

        return connectionFactory.useConnection {
            createNamedStatement(sql, mapOf("token" to token))
                .execute()
                .awaitSingle()
                .map(rowMapper)
                .awaitFirstOrNull()
        }
    }

    // Register or update device token
    suspend fun registerToken(
        userId: String,
        token: String,
        platform: String = "web"
    ): DeviceToken {
        // Deactivate old token if exists
        val existingToken = findByToken(token)
        if (existingToken != null) {
            if (existingToken.userId != userId || !existingToken.isActive) {
                // Reactivate and reassign token
                return update(
                    existingToken.id,
                    existingToken.copy(
                        userId = userId,
                        platform = platform,
                        isActive = true
                    )
                )!!
            }
            return existingToken
        }

        // Create new token
        return create(
            DeviceToken(
                userId = userId,
                token = token,
                platform = platform,
                isActive = true
            )
        )
    }

    // Deactivate a token
    suspend fun deactivateToken(token: String): Boolean {
        val sql = """
            UPDATE $tableName 
            SET is_active = false 
            WHERE token = :token
        """.trimIndent()

        return connectionFactory.withTransaction { connection ->
            connection.createNamedStatement(sql, mapOf("token" to token))
                .execute()
                .awaitSingle()
                .rowsUpdated
                .awaitSingle() > 0
        }
    }

    // Deactivate all tokens for a user
    suspend fun deactivateAllUserTokens(userId: String): Int {
        val sql = """
            UPDATE $tableName 
            SET is_active = false 
            WHERE user_id = :userId 
              AND is_active = true
        """.trimIndent()

        return connectionFactory.withTransaction { connection ->
            connection.createNamedStatement(sql, mapOf("userId" to userId))
                .execute()
                .awaitSingle()
                .rowsUpdated
                .awaitSingle()
                .toInt()
        }
    }

    // Get tokens by platform
    suspend fun findByPlatform(
        platform: String,
        isActive: Boolean = true,
        offset: Int = 0,
        limit: Int = 100
    ): List<DeviceToken> {
        val sql = """
            SELECT * FROM $tableName 
            WHERE platform = :platform 
              AND is_active = :isActive 
            ORDER BY created_at DESC 
            LIMIT :limit OFFSET :offset
        """.trimIndent()

        return executeQuery(
            sql, mapOf(
                "platform" to platform,
                "isActive" to isActive,
                "limit" to limit,
                "offset" to offset.toLong()
            )
        )
    }

    // Get active tokens for multiple users (for bulk notifications)
    suspend fun findActiveTokensByUserIds(userIds: List<String>): List<DeviceToken> {
        if (userIds.isEmpty()) return emptyList()

        val placeholders = List(userIds.size) { index -> ":userId$index" }

        val sql = """
            SELECT * FROM $tableName 
            WHERE user_id IN (${placeholders.joinToString(", ")}) 
              AND is_active = true 
            ORDER BY user_id, platform
        """.trimIndent()

        val params = mutableMapOf<String, Any>()

        userIds.forEachIndexed { index, userId ->
            params["userId$index"] = userId
        }

        return connectionFactory.useConnection {
            val statement = createNamedStatement(sql, params)

            statement.execute()
                .awaitSingle()
                .map(rowMapper)
                .asFlow()
                .toList()
        }
    }

    // Get token statistics
    suspend fun getTokenStats(): TokenStats {
        val sql = """
            SELECT 
                COUNT(*) as total_tokens,
                COUNT(CASE WHEN is_active = true THEN 1 END) as active_tokens,
                COUNT(DISTINCT user_id) as unique_users,
                COUNT(CASE WHEN platform = 'ANDROID' THEN 1 END) as android_count,
                COUNT(CASE WHEN platform = 'IOS' THEN 1 END) as ios_count,
                COUNT(CASE WHEN platform = 'WEB' THEN 1 END) as web_count
            FROM $tableName 
            WHERE is_active = true
        """.trimIndent()

        return connectionFactory.useConnection {
            createStatement(sql)
                .execute()
                .awaitSingle()
                .map { row, _ ->
                    TokenStats(
                        totalTokens = row.get("total_tokens", Long::class.java)!!.toInt(),
                        activeTokens = row.get("active_tokens", Long::class.java)!!.toInt(),
                        uniqueUsers = row.get("unique_users", Long::class.java)!!.toInt(),
                        androidCount = row.get("android_count", Long::class.java)!!.toInt(),
                        iosCount = row.get("ios_count", Long::class.java)!!.toInt(),
                        webCount = row.get("web_count", Long::class.java)!!.toInt()
                    )
                }
                .awaitFirstOrNull() ?: TokenStats(0, 0, 0, 0, 0, 0)
        }
    }

    // Cleanup inactive tokens older than specified days
    suspend fun cleanupInactiveTokens(olderThanDays: Int): Int {
        val sql = """
            DELETE FROM $tableName 
            WHERE is_active = false 
              AND created_at < :cutoffDate
        """.trimIndent()

        return connectionFactory.withTransaction { connection ->
            connection.createNamedStatement(
                sql,
                mapOf("cutoffDate" to OffsetDateTime.now().minusDays(olderThanDays.toLong()))
            )
                .execute()
                .awaitSingle()
                .rowsUpdated
                .awaitSingle()
                .toInt()
        }
    }
}

@Serializable
data class TokenStats(
    val totalTokens: Int,
    val activeTokens: Int,
    val uniqueUsers: Int,
    val androidCount: Int,
    val iosCount: Int,
    val webCount: Int
)