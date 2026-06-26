package org.example.data.repo

import io.r2dbc.spi.ConnectionFactory
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import org.example.data.mappers.CustomProductRequestMapper
import org.example.domain.models.customization.CustomProductRequest
import java.math.BigDecimal
import java.time.OffsetDateTime

class CustomProductRequestRepository(
    connectionFactory: ConnectionFactory,
    customProductRequestMapper: CustomProductRequestMapper
) : CrudRepository<CustomProductRequest, String>(
    connectionFactory = connectionFactory,
    tableName = "custom_product_requests",
    idColumn = "id",
    mapper = customProductRequestMapper
) {
    override val generatedColumns = listOf("id", "created_at", "updated_at")

    // Get requests by user
    suspend fun findByUserId(
        userId: String,
        offset: Int = 0,
        limit: Int = 20
    ): List<CustomProductRequest> {
        val sql = """
            SELECT * FROM $tableName 
            WHERE user_id = :userId 
            ORDER BY created_at DESC 
            LIMIT :limit OFFSET :offset
        """.trimIndent()

        return executeQuery(sql, mapOf(
            "userId" to userId,
            "limit" to limit,
            "offset" to offset.toLong()
        ))
    }

    // Get requests by status
    suspend fun findByStatus(
        status: String,
        offset: Int = 0,
        limit: Int = 20
    ): List<CustomProductRequest> {
        val sql = """
            SELECT * FROM $tableName 
            WHERE status = :status 
            ORDER BY created_at DESC 
            LIMIT :limit OFFSET :offset
        """.trimIndent()

        return executeQuery(sql, mapOf(
            "status" to status,
            "limit" to limit,
            "offset" to offset.toLong()
        ))
    }

    // Search requests by title or description
    suspend fun search(
        query: String,
        status: String? = null,
        offset: Int = 0,
        limit: Int = 20
    ): List<CustomProductRequest> {
        val statusFilter = if (status != null) "AND status = :status" else ""
        val sql = """
            SELECT * FROM $tableName 
            WHERE (title ILIKE :query OR description ILIKE :query) 
            $statusFilter
            ORDER BY created_at DESC 
            LIMIT :limit OFFSET :offset
        """.trimIndent()

        val params = mutableMapOf(
            "query" to "%$query%",
            "limit" to limit,
            "offset" to offset.toLong()
        )

        if (status != null) {
            params["status"] = status
        }

        return executeQuery(sql, params)
    }

    // Update request status
    suspend fun updateStatus(
        id: String,
        status: String,
        notes: String? = null
    ): CustomProductRequest? {
        val sql = """
            UPDATE $tableName 
            SET status = :status, 
                notes = COALESCE(:notes, notes),
                updated_at = :updatedAt 
            WHERE id = :id 
            RETURNING *
        """.trimIndent()

        return connectionFactory.withTransaction { connection ->
            val statement = connection.createStatement(sql)
                .bind("id", id)
                .bind("status", status)
                .bind("updatedAt", OffsetDateTime.now())

            if (notes != null) {
                statement.bind("notes", notes)
            } else {
                statement.bindNull("notes", String::class.java)
            }

            statement.execute()
                .awaitSingle()
                .map(rowMapper)
                .awaitFirstOrNull()
        }
    }

    // Get requests with budget range
    suspend fun findByBudgetRange(
        minBudget: BigDecimal? = null,
        maxBudget: BigDecimal? = null,
        offset: Int = 0,
        limit: Int = 20
    ): List<CustomProductRequest> {
        val conditions = mutableListOf<String>()
        val params = mutableMapOf<String, Any>(
            "limit" to limit,
            "offset" to offset.toLong()
        )

        minBudget?.let {
            conditions.add("estimated_budget_max >= :minBudget")
            params["minBudget"] = it
        }

        maxBudget?.let {
            conditions.add("estimated_budget_min <= :maxBudget")
            params["maxBudget"] = it
        }

        val whereClause = if (conditions.isNotEmpty()) {
            "WHERE ${conditions.joinToString(" AND ")}"
        } else {
            ""
        }

        val sql = """
            SELECT * FROM $tableName 
            $whereClause
            ORDER BY created_at DESC 
            LIMIT :limit OFFSET :offset
        """.trimIndent()

        return executeQuery(sql, params)
    }

    // Count requests by status
    suspend fun countByStatus(): Map<String, Int> {
        val sql = """
            SELECT status, COUNT(*) as count 
            FROM $tableName 
            GROUP BY status
        """.trimIndent()

        return connectionFactory.useConnection {
            createStatement(sql)
                .execute()
                .awaitSingle()
                .map { row, _ ->
                    row.get("status", String::class.java) to
                            row.get("count", Long::class.java)!!.toInt()
                }
                .asFlow()
                .toList()
                .toMap()
        }
    }

    // Get recent requests
    suspend fun getRecentRequests(limit: Int = 10): List<CustomProductRequest> {
        val sql = """
            SELECT * FROM $tableName 
            ORDER BY created_at DESC 
            LIMIT :limit
        """.trimIndent()

        return executeQuery(sql, mapOf("limit" to limit))
    }

    // Assign request to staff
    suspend fun assignTo(
        requestId: String,
        assigneeId: String,
        notes: String? = null
    ): CustomProductRequest? {
        val sql = """
            UPDATE $tableName 
            SET status = 'in_progress',
                notes = CASE 
                    WHEN notes IS NOT NULL AND :notes IS NOT NULL 
                    THEN CONCAT(notes, E'\n', :notes)
                    ELSE COALESCE(:notes, notes)
                END,
                updated_at = :updatedAt 
            WHERE id = :requestId 
              AND status = 'pending'
            RETURNING *
        """.trimIndent()

        return connectionFactory.withTransaction { connection ->
            connection.createStatement(sql)
                .bind("requestId", requestId)
                .bind("updatedAt", OffsetDateTime.now())
                .bind("notes", notes)
                .execute()
                .awaitSingle()
                .map(rowMapper)
                .awaitFirstOrNull()
        }
    }

    // Get user's request statistics
    suspend fun getUserRequestStats(userId: String): UserRequestStats {
        val sql = """
            SELECT 
                COUNT(*) as total_requests,
                COUNT(CASE WHEN status = 'pending' THEN 1 END) as pending,
                COUNT(CASE WHEN status = 'in_progress' THEN 1 END) as in_progress,
                COUNT(CASE WHEN status = 'completed' THEN 1 END) as completed,
                COUNT(CASE WHEN status = 'cancelled' THEN 1 END) as cancelled
            FROM $tableName 
            WHERE user_id = :userId
        """.trimIndent()

        return connectionFactory.useConnection {
            createStatement(sql)
                .bind("userId", userId)
                .execute()
                .awaitSingle()
                .map { row, _ ->
                    UserRequestStats(
                        totalRequests = row.get("total_requests", Long::class.java)!!.toInt(),
                        pending = row.get("pending", Long::class.java)!!.toInt(),
                        inProgress = row.get("in_progress", Long::class.java)!!.toInt(),
                        completed = row.get("completed", Long::class.java)!!.toInt(),
                        cancelled = row.get("cancelled", Long::class.java)!!.toInt()
                    )
                }
                .awaitFirstOrNull() ?: UserRequestStats(0, 0, 0, 0, 0)
        }
    }
}

data class UserRequestStats(
    val totalRequests: Int,
    val pending: Int,
    val inProgress: Int,
    val completed: Int,
    val cancelled: Int
)