package org.example.data.repo

import io.r2dbc.spi.ConnectionFactory
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import org.example.data.mappers.AuditLogsMapper
import org.example.domain.models.system.AuditLogs
import java.net.InetAddress
import java.time.OffsetDateTime

class AuditLogsRepository(
    connectionFactory: ConnectionFactory,
    auditLogsMapper: AuditLogsMapper
) : CrudRepository<AuditLogs, String>(
    connectionFactory = connectionFactory,
    tableName = "audit_logs",
    idColumn = "id",
    mapper = auditLogsMapper
) {
    override val generatedColumns = listOf("id", "created_at")

    // Get audit logs by entity
    suspend fun findByEntity(
        entityType: String,
        entityId: String,
        offset: Int = 0,
        limit: Int = 50
    ): List<AuditLogs> {
        val sql = """
            SELECT * FROM $tableName 
            WHERE entity_type = :entityType 
              AND entity_id = :entityId 
            ORDER BY created_at DESC 
            LIMIT :limit OFFSET :offset
        """.trimIndent()

        return executeQuery(sql, mapOf(
            "entityType" to entityType,
            "entityId" to entityId,
            "limit" to limit,
            "offset" to offset.toLong()
        ))
    }

    // Get audit logs by actor
    suspend fun findByActor(
        actorId: String,
        actorType: String? = null,
        offset: Int = 0,
        limit: Int = 50
    ): List<AuditLogs> {
        val typeFilter = if (actorType != null) "AND actor_type = :actorType" else ""

        val sql = """
            SELECT * FROM $tableName 
            WHERE actor_id = :actorId 
            $typeFilter
            ORDER BY created_at DESC 
            LIMIT :limit OFFSET :offset
        """.trimIndent()

        val params = mutableMapOf(
            "actorId" to actorId,
            "limit" to limit,
            "offset" to offset.toLong()
        )

        if (actorType != null) {
            params["actorType"] = actorType
        }

        return executeQuery(sql, params)
    }

    // Get audit logs by action
    suspend fun findByAction(
        action: String,
        entityType: String? = null,
        offset: Int = 0,
        limit: Int = 50
    ): List<AuditLogs> {
        val entityFilter = if (entityType != null) "AND entity_type = :entityType" else ""

        val sql = """
            SELECT * FROM $tableName 
            WHERE action = :action 
            $entityFilter
            ORDER BY created_at DESC 
            LIMIT :limit OFFSET :offset
        """.trimIndent()

        val params = mutableMapOf(
            "action" to action,
            "limit" to limit,
            "offset" to offset.toLong()
        )

        if (entityType != null) {
            params["entityType"] = entityType
        }

        return executeQuery(sql, params)
    }

    // Get audit logs by date range
    suspend fun findByDateRange(
        startDate: OffsetDateTime,
        endDate: OffsetDateTime,
        action: String? = null,
        entityType: String? = null,
        actorId: String? = null,
        offset: Int = 0,
        limit: Int = 50
    ): List<AuditLogs> {
        val conditions = mutableListOf("created_at BETWEEN :startDate AND :endDate")
        val params = mutableMapOf<String, Any>(
            "startDate" to startDate,
            "endDate" to endDate,
            "limit" to limit,
            "offset" to offset.toLong()
        )

        action?.let {
            conditions.add("action = :action")
            params["action"] = it
        }

        entityType?.let {
            conditions.add("entity_type = :entityType")
            params["entityType"] = it
        }

        actorId?.let {
            conditions.add("actor_id = :actorId")
            params["actorId"] = it
        }

        val sql = """
            SELECT * FROM $tableName 
            WHERE ${conditions.joinToString(" AND ")} 
            ORDER BY created_at DESC 
            LIMIT :limit OFFSET :offset
        """.trimIndent()

        return executeQuery(sql, params)
    }

    // Log an action
    suspend fun logAction(
        actorId: String?,
        actorType: String,
        action: String,
        entityType: String,
        entityId: String,
        changes: Map<String, Any>? = null,
        metadata: Map<String, Any>? = null,
        ipAddress: InetAddress? = null,
        userAgent: String? = null
    ): AuditLogs {
        return create(
            AuditLogs(
                actorId = actorId,
                actorType = actorType,
                action = action,
                entityType = entityType,
                entityId = entityId,
                changes = changes,
                metadata = metadata,
                ipAddress = ipAddress,
                userAgent = userAgent
            )
        )
    }

    // Get recent activity for an entity
    suspend fun getRecentActivity(
        entityType: String,
        entityId: String,
        limit: Int = 10
    ): List<AuditLogs> {
        val sql = """
            SELECT * FROM $tableName 
            WHERE entity_type = :entityType 
              AND entity_id = :entityId 
            ORDER BY created_at DESC 
            LIMIT :limit
        """.trimIndent()

        return executeQuery(sql, mapOf(
            "entityType" to entityType,
            "entityId" to entityId,
            "limit" to limit
        ))
    }

    // Get audit statistics
    suspend fun getAuditStats(
        startDate: OffsetDateTime? = null,
        endDate: OffsetDateTime? = null
    ): AuditStats {
        val dateFilter = if (startDate != null && endDate != null) {
            "WHERE created_at BETWEEN :startDate AND :endDate"
        } else ""

        val sql = """
            SELECT 
                COUNT(*) as total_logs,
                COUNT(DISTINCT actor_id) as unique_actors,
                COUNT(DISTINCT entity_type) as unique_entities,
                COUNT(DISTINCT action) as unique_actions,
                COUNT(DISTINCT DATE(created_at)) as active_days
            FROM $tableName 
            $dateFilter
        """.trimIndent()

        return connectionFactory.useConnection {
            val statement = createStatement(sql)
            if (startDate != null && endDate != null) {
                statement.bind("startDate", startDate)
                statement.bind("endDate", endDate)
            }

            statement.execute()
                .awaitSingle()
                .map { row, _ ->
                    AuditStats(
                        totalLogs = row.get("total_logs", Long::class.java)!!.toInt(),
                        uniqueActors = row.get("unique_actors", Long::class.java)!!.toInt(),
                        uniqueEntities = row.get("unique_entities", Long::class.java)!!.toInt(),
                        uniqueActions = row.get("unique_actions", Long::class.java)!!.toInt(),
                        activeDays = row.get("active_days", Long::class.java)!!.toInt()
                    )
                }
                .awaitFirstOrNull() ?: AuditStats(0, 0, 0, 0, 0)
        }
    }

    // Get top actions
    suspend fun getTopActions(
        limit: Int = 10,
        startDate: OffsetDateTime? = null,
        endDate: OffsetDateTime? = null
    ): List<ActionCount> {
        val dateFilter = if (startDate != null && endDate != null) {
            "AND created_at BETWEEN :startDate AND :endDate"
        } else ""

        val sql = """
            SELECT 
                action,
                COUNT(*) as action_count,
                COUNT(DISTINCT actor_id) as unique_actors
            FROM $tableName 
            WHERE 1=1 $dateFilter
            GROUP BY action
            ORDER BY action_count DESC 
            LIMIT :limit
        """.trimIndent()

        return connectionFactory.useConnection {
            val statement = createStatement(sql)
                .bind("limit", limit)

            if (startDate != null && endDate != null) {
                statement.bind("startDate", startDate)
                statement.bind("endDate", endDate)
            }

            statement.execute()
                .awaitSingle()
                .map { row, _ ->
                    ActionCount(
                        action = row.get("action", String::class.java)!!,
                        count = row.get("action_count", Long::class.java)!!.toInt(),
                        uniqueActors = row.get("unique_actors", Long::class.java)!!.toInt()
                    )
                }
                .asFlow()
                .toList()
        }
    }

    // Cleanup old audit logs
    suspend fun cleanupOldLogs(olderThanDays: Int): Int {
        val sql = """
            DELETE FROM $tableName 
            WHERE created_at < :cutoffDate
        """.trimIndent()

        return connectionFactory.withTransaction { connection ->
            connection.createStatement(sql)
                .bind("cutoffDate", OffsetDateTime.now().minusDays(olderThanDays.toLong()))
                .execute()
                .awaitSingle()
                .rowsUpdated
                .awaitSingle() as Int
        }
    }

    // Search audit logs
    suspend fun searchLogs(
        query: String,
        offset: Int = 0,
        limit: Int = 50
    ): List<AuditLogs> {
        val sql = """
            SELECT * FROM $tableName 
            WHERE action ILIKE :query 
               OR entity_type ILIKE :query 
               OR actor_type ILIKE :query 
            ORDER BY created_at DESC 
            LIMIT :limit OFFSET :offset
        """.trimIndent()

        return executeQuery(sql, mapOf(
            "query" to "%$query%",
            "limit" to limit,
            "offset" to offset.toLong()
        ))
    }

    // Get changes for entity over time
    suspend fun getEntityChanges(
        entityType: String,
        entityId: String
    ): List<EntityChange> {
        val sql = """
            SELECT 
                id,
                action,
                changes,
                actor_id,
                actor_type,
                created_at
            FROM $tableName 
            WHERE entity_type = :entityType 
              AND entity_id = :entityId 
              AND changes IS NOT NULL
            ORDER BY created_at ASC
        """.trimIndent()

        return connectionFactory.useConnection {
            createStatement(sql)
                .bind("entityType", entityType)
                .bind("entityId", entityId)
                .execute()
                .awaitSingle()
                .map { row, _ ->
                    EntityChange(
                        id = row.get("id", String::class.java)!!,
                        action = row.get("action", String::class.java)!!,
                        changes = row.get("changes", Map::class.java) as? Map<String, Any>,
                        actorId = row.get("actor_id", String::class.java),
                        actorType = row.get("actor_type", String::class.java)!!,
                        createdAt = row.get("created_at", OffsetDateTime::class.java)!!
                    )
                }
                .asFlow()
                .toList()
        }
    }
}

data class AuditStats(
    val totalLogs: Int,
    val uniqueActors: Int,
    val uniqueEntities: Int,
    val uniqueActions: Int,
    val activeDays: Int
)

data class ActionCount(
    val action: String,
    val count: Int,
    val uniqueActors: Int
)

data class EntityChange(
    val id: String,
    val action: String,
    val changes: Map<String, Any>?,
    val actorId: String?,
    val actorType: String,
    val createdAt: OffsetDateTime
)