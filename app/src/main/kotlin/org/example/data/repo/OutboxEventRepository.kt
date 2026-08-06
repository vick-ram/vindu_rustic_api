package org.example.data.repo

import io.r2dbc.spi.Connection
import io.r2dbc.spi.ConnectionFactory
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.serialization.Serializable
import org.example.data.mappers.OutboxEventMapper
import org.example.di.Component
import org.example.di.Inject
import org.example.domain.models.system.OutboxEvent
import java.time.OffsetDateTime

@Component
class OutboxEventRepository @Inject constructor(
    connectionFactory: ConnectionFactory,
    outboxEventMapper: OutboxEventMapper
) : CrudRepository<OutboxEvent, String>(
    connectionFactory = connectionFactory,
    tableName = "outbox_events",
    mapper = outboxEventMapper
) {
    override val generatedColumns = listOf("id", "created_at")

    // Get unprocessed events
    suspend fun findUnprocessed(
        limit: Int = 100,
        maxAttempts: Int = 3
    ): List<OutboxEvent> {
        val sql = """
            SELECT * FROM $tableName 
            WHERE processed = false 
              AND attempts < :maxAttempts 
            ORDER BY created_at ASC 
            LIMIT :limit
            FOR UPDATE SKIP LOCKED
        """.trimIndent()

        return connectionFactory.withTransaction { connection ->
            connection.createNamedStatement(sql, mapOf("limit" to limit, "maxAttempts" to maxAttempts))
                .execute()
                .awaitSingle()
                .map(rowMapper)
                .asFlow()
                .toList()
        }
    }

    // Get events by aggregate
    suspend fun findByAggregate(
        aggregateType: String,
        aggregateId: String,
        offset: Int = 0,
        limit: Int = 50
    ): List<OutboxEvent> {
        val sql = """
            SELECT * FROM $tableName 
            WHERE aggregate_type = :aggregateType 
              AND aggregate_id = :aggregateId 
            ORDER BY created_at DESC 
            LIMIT :limit OFFSET :offset
        """.trimIndent()

        return executeQuery(
            sql, mapOf(
                "aggregateType" to aggregateType,
                "aggregateId" to aggregateId,
                "limit" to limit,
                "offset" to offset.toLong()
            )
        )
    }

    // Get events by type
    suspend fun findByEventType(
        eventType: String,
        offset: Int = 0,
        limit: Int = 50
    ): List<OutboxEvent> {
        val sql = """
            SELECT * FROM $tableName 
            WHERE event_type = :eventType 
            ORDER BY created_at DESC 
            LIMIT :limit OFFSET :offset
        """.trimIndent()

        return executeQuery(
            sql, mapOf(
                "eventType" to eventType,
                "limit" to limit,
                "offset" to offset.toLong()
            )
        )
    }

    // Mark event as processed
    suspend fun markAsProcessed(id: String): Boolean {
        val sql = """
            UPDATE $tableName 
            SET processed = true,
                processed_at = :processedAt 
            WHERE id = :id 
              AND processed = false
        """.trimIndent()

        return connectionFactory.withTransaction { connection ->
            connection.createNamedStatement(sql, mapOf("id" to id, "processedAt" to OffsetDateTime.now()))
                .execute()
                .awaitSingle()
                .rowsUpdated
                .awaitSingle() > 0
        }
    }

    // Mark event as failed with error
    suspend fun markAsFailed(
        id: String,
        errorMessage: String
    ): Boolean {
        val sql = """
            UPDATE $tableName 
            SET attempts = attempts + 1,
                error_message = :errorMessage 
            WHERE id = :id 
              AND processed = false
        """.trimIndent()

        return connectionFactory.withTransaction { connection ->
            connection.createNamedStatement(sql, mapOf("id" to id, "errorMessage" to errorMessage))
                .execute()
                .awaitSingle()
                .rowsUpdated
                .awaitSingle() > 0
        }
    }

    // Reset failed events for retry
    suspend fun resetFailedEvents(
        olderThan: OffsetDateTime? = null,
        maxAttempts: Int = 3
    ): Int {
        val timeFilter = if (olderThan != null) {
            "AND created_at > :olderThan"
        } else ""

        val sql = """
            UPDATE $tableName 
            SET attempts = 0,
                error_message = NULL 
            WHERE processed = false 
              AND attempts >= :maxAttempts 
              $timeFilter
        """.trimIndent()

        return connectionFactory.withTransaction { connection ->
            val statement = connection.createNamedStatement(sql, mapOf("maxAttempts" to maxAttempts))

            if (olderThan != null) {
                statement.bind("olderThan", olderThan)
            }

            statement.execute()
                .awaitSingle()
                .rowsUpdated
                .awaitSingle()
                .toInt()
        }
    }

    // Publish event (insert into outbox)
    suspend fun publishEvent(
        aggregateType: String,
        aggregateId: String,
        eventType: String,
        payload: Map<String, Any>,
        connection: Connection? = null
    ): OutboxEvent {
        return create(
            OutboxEvent(
                aggregateType = aggregateType,
                aggregateId = aggregateId,
                eventType = eventType,
                payload = payload
            ),
            connection
        )
    }

    // Get event statistics
    suspend fun getEventStats(): OutboxStats {
        val sql = """
            SELECT 
                COUNT(*) as total_events,
                COUNT(CASE WHEN processed = true THEN 1 END) as processed,
                COUNT(CASE WHEN processed = false THEN 1 END) as pending,
                COUNT(CASE WHEN attempts >= 3 AND processed = false THEN 1 END) as failed,
                COUNT(DISTINCT event_type) as unique_event_types,
                COUNT(DISTINCT aggregate_type) as unique_aggregate_types
            FROM $tableName
        """.trimIndent()

        return connectionFactory.useConnection {
            createStatement(sql)
                .execute()
                .awaitSingle()
                .map { row, _ ->
                    OutboxStats(
                        totalEvents = row.get("total_events", Long::class.java)!!.toInt(),
                        processed = row.get("processed", Long::class.java)!!.toInt(),
                        pending = row.get("pending", Long::class.java)!!.toInt(),
                        failed = row.get("failed", Long::class.java)!!.toInt(),
                        uniqueEventTypes = row.get("unique_event_types", Long::class.java)!!.toInt(),
                        uniqueAggregateTypes = row.get("unique_aggregate_types", Long::class.java)!!.toInt()
                    )
                }
                .awaitFirstOrNull() ?: OutboxStats(0, 0, 0, 0, 0, 0)
        }
    }

    // Cleanup processed events older than specified days
    suspend fun cleanupProcessedEvents(olderThanDays: Int): Int {
        val sql = """
            DELETE FROM $tableName 
            WHERE processed = true 
              AND processed_at < :cutoffDate
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

    // Get events by date range
    suspend fun findByDateRange(
        startDate: OffsetDateTime,
        endDate: OffsetDateTime,
        processed: Boolean? = null,
        eventType: String? = null,
        offset: Int = 0,
        limit: Int = 50
    ): List<OutboxEvent> {
        val conditions = mutableListOf("created_at BETWEEN :startDate AND :endDate")
        val params = mutableMapOf<String, Any>(
            "startDate" to startDate,
            "endDate" to endDate,
            "limit" to limit,
            "offset" to offset.toLong()
        )

        processed?.let {
            conditions.add("processed = :processed")
            params["processed"] = it
        }

        eventType?.let {
            conditions.add("event_type = :eventType")
            params["eventType"] = it
        }

        val sql = """
            SELECT * FROM $tableName 
            WHERE ${conditions.joinToString(" AND ")} 
            ORDER BY created_at DESC 
            LIMIT :limit OFFSET :offset
        """.trimIndent()

        return executeQuery(sql, params)
    }

    // Get pending events count by type
    suspend fun getPendingCountByType(): Map<String, Int> {
        val sql = """
            SELECT 
                event_type,
                COUNT(*) as count
            FROM $tableName 
            WHERE processed = false 
              AND attempts < 3
            GROUP BY event_type
            ORDER BY count DESC
        """.trimIndent()

        return connectionFactory.useConnection {
            createStatement(sql)
                .execute()
                .awaitSingle()
                .map { row, _ ->
                    row.get("event_type", String::class.java) to
                            row.get("count", Long::class.java)!!.toInt()
                }
                .asFlow()
                .toList()
                .toMap()
        }
    }
}

@Serializable
data class OutboxStats(
    val totalEvents: Int,
    val processed: Int,
    val pending: Int,
    val failed: Int,
    val uniqueEventTypes: Int,
    val uniqueAggregateTypes: Int
)