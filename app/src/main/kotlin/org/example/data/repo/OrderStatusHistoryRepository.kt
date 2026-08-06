package org.example.data.repo

import io.r2dbc.spi.ConnectionFactory
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.example.data.mappers.OrderStatusHistoryMapper
import org.example.di.Component
import org.example.di.Inject
import org.example.domain.models.sales.OrderStatusHistory
import java.time.OffsetDateTime

@Component
class OrderStatusHistoryRepository @Inject constructor(
    connectionFactory: ConnectionFactory,
    orderStatusHistoryMapper: OrderStatusHistoryMapper
) : CrudRepository<OrderStatusHistory, String>(
    connectionFactory = connectionFactory,
    tableName = "order_status_history",
    idColumn = "id",
    mapper = orderStatusHistoryMapper
) {
    override val generatedColumns = listOf("id", "created_at")

    // Get status history for an order
    suspend fun findByOrderId(orderId: String): List<OrderStatusHistory> {
        val sql = """
            SELECT * FROM $tableName 
            WHERE order_id = :orderId 
            ORDER BY created_at DESC
        """.trimIndent()

        return executeQuery(sql, mapOf("orderId" to orderId))
    }

    // Get latest status change for an order
    suspend fun getLatestStatusChange(orderId: String): OrderStatusHistory? {
        val sql = """
            SELECT * FROM $tableName 
            WHERE order_id = :orderId 
            ORDER BY created_at DESC 
            LIMIT 1
        """.trimIndent()

        return connectionFactory.useConnection {
            createNamedStatement(sql, mapOf("orderId" to orderId))
                .execute()
                .awaitSingle()
                .map(rowMapper)
                .awaitFirstOrNull()
        }
    }

    // Get status changes by user
    suspend fun findByChangedBy(
        changedBy: String,
        offset: Int = 0,
        limit: Int = 50
    ): List<OrderStatusHistory> {
        val sql = """
            SELECT * FROM $tableName 
            WHERE changed_by = :changedBy 
            ORDER BY created_at DESC 
            LIMIT :limit OFFSET :offset
        """.trimIndent()

        return executeQuery(sql, mapOf(
            "changedBy" to changedBy,
            "limit" to limit,
            "offset" to offset.toLong()
        ))
    }

    // Get time spent in each status
    suspend fun getStatusDuration(orderId: String): List<StatusDuration> {
        val sql = """
            WITH status_changes AS (
                SELECT 
                    new_status as status,
                    created_at as changed_at,
                    LEAD(created_at) OVER (ORDER BY created_at) as next_change_at
                FROM $tableName 
                WHERE order_id = :orderId
                ORDER BY created_at
            )
            SELECT 
                status,
                changed_at,
                next_change_at,
                EXTRACT(EPOCH FROM (next_change_at - changed_at)) / 3600.0 as duration_hours
            FROM status_changes
            WHERE next_change_at IS NOT NULL
            ORDER BY changed_at
        """.trimIndent()

        return connectionFactory.useConnection {
            createNamedStatement(sql, mapOf("orderId" to orderId))
                .execute()
                .awaitSingle()
                .map { row, _ ->
                    StatusDuration(
                        status = row.get("status", String::class.java)!!,
                        changedAt = row.get("changed_at", OffsetDateTime::class.java)!!,
                        nextChangeAt = row.get("next_change_at", OffsetDateTime::class.java)!!,
                        durationHours = row.get("duration_hours", Double::class.java) ?: 0.0
                    )
                }
                .asFlow()
                .toList()
        }
    }

    // Log status change
    suspend fun logStatusChange(
        orderId: String,
        oldStatus: String?,
        newStatus: String,
        changedBy: String? = null,
        comment: String? = null
    ): OrderStatusHistory {
        return create(
            OrderStatusHistory(
                orderId = orderId,
                oldStatus = oldStatus,
                newStatus = newStatus,
                changedBy = changedBy,
                comment = comment
            )
        )
    }
}

@Serializable
data class StatusDuration(
    val status: String,
    @Contextual val changedAt: OffsetDateTime,
    @Contextual val nextChangeAt: OffsetDateTime,
    val durationHours: Double
)