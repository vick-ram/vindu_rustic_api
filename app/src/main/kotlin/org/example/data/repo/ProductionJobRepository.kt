package org.example.data.repo

import io.r2dbc.spi.ConnectionFactory
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.serialization.Serializable
import org.example.data.mappers.ProductionJobMapper
import org.example.di.Component
import org.example.di.Inject
import org.example.domain.models.production.ProductionJob
import org.koin.core.annotation.Single
import java.time.OffsetDateTime

@Component
class ProductionJobRepository @Inject constructor(
    connectionFactory: ConnectionFactory,
    productionJobMapper: ProductionJobMapper
) : CrudRepository<ProductionJob, String>(
    connectionFactory = connectionFactory,
    tableName = "production_jobs",
    mapper = productionJobMapper
) {
    override val generatedColumns = listOf("id", "created_at", "updated_at")

    // Override update to handle updated_at
    override suspend fun update(id: String, model: ProductionJob): ProductionJob? {
        validateProductionJob(model)
        return super.update(id, model)
    }

    // Create with validation
    override suspend fun create(model: ProductionJob): ProductionJob {
        validateProductionJob(model)
        return super.create(model)
    }

    // Get jobs by status
    suspend fun findByStatus(
        status: String,
        offset: Int = 0,
        limit: Int = 20
    ): List<ProductionJob> {
        val sql = """
            SELECT * FROM $tableName 
            WHERE status = :status 
            ORDER BY 
                CASE priority 
                    WHEN 'URGENT' THEN 1 
                    WHEN 'HIGH' THEN 2 
                    WHEN 'NORMAL' THEN 3 
                    WHEN 'LOW' THEN 4 
                    ELSE 5 
                END,
                created_at ASC 
            LIMIT :limit OFFSET :offset
        """.trimIndent()

        return executeQuery(sql, mapOf(
            "status" to status,
            "limit" to limit,
            "offset" to offset.toLong()
        ))
    }

    // Get jobs by priority
    suspend fun findByPriority(
        priority: String,
        offset: Int = 0,
        limit: Int = 20
    ): List<ProductionJob> {
        val sql = """
            SELECT * FROM $tableName 
            WHERE priority = :priority 
              AND status NOT IN ('COMPLETED', 'CANCELLED') 
            ORDER BY created_at ASC 
            LIMIT :limit OFFSET :offset
        """.trimIndent()

        return executeQuery(sql, mapOf(
            "priority" to priority,
            "limit" to limit,
            "offset" to offset.toLong()
        ))
    }

    // Get jobs assigned to a user
    suspend fun findByAssignedTo(
        assignedTo: String,
        includeCompleted: Boolean = false,
        offset: Int = 0,
        limit: Int = 20
    ): List<ProductionJob> {
        val statusFilter = if (!includeCompleted) {
            "AND status NOT IN ('COMPLETED', 'CANCELLED')"
        } else ""

        val sql = """
            SELECT * FROM $tableName 
            WHERE assigned_to = :assignedTo 
            $statusFilter
            ORDER BY 
                CASE priority 
                    WHEN 'URGENT' THEN 1 
                    WHEN 'HIGH' THEN 2 
                    WHEN 'NORMAL' THEN 3 
                    WHEN 'LOW' THEN 4 
                    ELSE 5 
                END,
                created_at ASC 
            LIMIT :limit OFFSET :offset
        """.trimIndent()

        return executeQuery(sql, mapOf(
            "assignedTo" to assignedTo,
            "limit" to limit,
            "offset" to offset.toLong()
        ))
    }

    // Get jobs for an order item
    suspend fun findByOrderItemId(orderItemId: String): List<ProductionJob> {
        val sql = """
            SELECT * FROM $tableName 
            WHERE order_item_id = :orderItemId 
            ORDER BY created_at DESC
        """.trimIndent()

        return executeQuery(sql, mapOf("orderItemId" to orderItemId))
    }

    // Get jobs for a product
    suspend fun findByProductId(
        productId: String,
        offset: Int = 0,
        limit: Int = 20
    ): List<ProductionJob> {
        val sql = """
            SELECT * FROM $tableName 
            WHERE product_id = :productId 
            ORDER BY created_at DESC 
            LIMIT :limit OFFSET :offset
        """.trimIndent()

        return executeQuery(sql, mapOf(
            "productId" to productId,
            "limit" to limit,
            "offset" to offset.toLong()
        ))
    }

    // Start a job
    suspend fun startJob(id: String, assignedTo: String? = null): ProductionJob? {
        val sql = """
            UPDATE $tableName 
            SET status = 'IN_PROGRESS',
                assigned_to = COALESCE(:assignedTo, assigned_to),
                started_at = :startedAt,
                updated_at = :updatedAt
            WHERE id = :id 
              AND status = 'QUEUED'
            RETURNING *
        """.trimIndent()

        return connectionFactory.withTransaction { connection ->
            val now = OffsetDateTime.now()
            connection.createStatement(sql)
                .bind("id", id)
                .bind("startedAt", now)
                .bind("updatedAt", now)
                .apply {
                    if (assignedTo != null) {
                        bind("assignedTo", assignedTo)
                    } else {
                        bindNull("assignedTo", String::class.java)
                    }
                }
                .execute()
                .awaitSingle()
                .map(rowMapper)
                .awaitFirstOrNull()
        }
    }

    // Complete a job
    suspend fun completeJob(id: String): ProductionJob? {
        val sql = """
            UPDATE $tableName 
            SET status = 'COMPLETED',
                completed_at = :completedAt,
                updated_at = :updatedAt
            WHERE id = :id 
              AND status = 'IN_PROGRESS'
            RETURNING *
        """.trimIndent()

        return connectionFactory.withTransaction { connection ->
            val now = OffsetDateTime.now()
            connection.createStatement(sql)
                .bind("id", id)
                .bind("completedAt", now)
                .bind("updatedAt", now)
                .execute()
                .awaitSingle()
                .map(rowMapper)
                .awaitFirstOrNull()
        }
    }

    // Pause a job
    suspend fun pauseJob(id: String): ProductionJob? {
        val sql = """
            UPDATE $tableName 
            SET status = 'PAUSED',
                updated_at = :updatedAt
            WHERE id = :id 
              AND status = 'IN_PROGRESS'
            RETURNING *
        """.trimIndent()

        return connectionFactory.withTransaction { connection ->
            connection.createStatement(sql)
                .bind("id", id)
                .bind("updatedAt", OffsetDateTime.now())
                .execute()
                .awaitSingle()
                .map(rowMapper)
                .awaitFirstOrNull()
        }
    }

    // Cancel a job
    suspend fun cancelJob(id: String, reason: String? = null): ProductionJob? {
        val sql = """
            UPDATE $tableName 
            SET status = 'CANCELLED',
                notes = CASE 
                    WHEN notes IS NOT NULL AND :reason IS NOT NULL 
                    THEN CONCAT(notes, E'\nCancelled: ', :reason)
                    ELSE COALESCE(:reason, notes)
                END,
                updated_at = :updatedAt
            WHERE id = :id 
              AND status NOT IN ('COMPLETED', 'CANCELLED')
            RETURNING *
        """.trimIndent()

        return connectionFactory.withTransaction { connection ->
            connection.createStatement(sql)
                .bind("id", id)
                .bind("updatedAt", OffsetDateTime.now())
                .apply {
                    if (reason != null) {
                        bind("reason", reason)
                    } else {
                        bindNull("reason", String::class.java)
                    }
                }
                .execute()
                .awaitSingle()
                .map(rowMapper)
                .awaitFirstOrNull()
        }
    }

    // Assign job to user
    suspend fun assignJob(id: String, assignedTo: String): ProductionJob? {
        val sql = """
            UPDATE $tableName 
            SET assigned_to = :assignedTo,
                updated_at = :updatedAt
            WHERE id = :id 
              AND status NOT IN ('COMPLETED', 'CANCELLED')
            RETURNING *
        """.trimIndent()

        return connectionFactory.withTransaction { connection ->
            connection.createStatement(sql)
                .bind("id", id)
                .bind("assignedTo", assignedTo)
                .bind("updatedAt", OffsetDateTime.now())
                .execute()
                .awaitSingle()
                .map(rowMapper)
                .awaitFirstOrNull()
        }
    }

    // Update job priority
    suspend fun updatePriority(id: String, priority: String): ProductionJob? {
        val validPriorities = listOf("LOW", "NORMAL", "HIGH", "URGENT")
        if (priority !in validPriorities) {
            throw IllegalArgumentException("Invalid priority: $priority. Must be one of: ${validPriorities.joinToString()}")
        }

        val sql = """
            UPDATE $tableName 
            SET priority = :priority,
                updated_at = :updatedAt
            WHERE id = :id 
            RETURNING *
        """.trimIndent()

        return connectionFactory.withTransaction { connection ->
            connection.createStatement(sql)
                .bind("id", id)
                .bind("priority", priority)
                .bind("updatedAt", OffsetDateTime.now())
                .execute()
                .awaitSingle()
                .map(rowMapper)
                .awaitFirstOrNull()
        }
    }

    // Get production queue (jobs waiting to be started)
    suspend fun getProductionQueue(limit: Int = 50): List<ProductionJob> {
        val sql = """
            SELECT * FROM $tableName 
            WHERE status = 'QUEUED' 
            ORDER BY 
                CASE priority 
                    WHEN 'URGENT' THEN 1 
                    WHEN 'HIGH' THEN 2 
                    WHEN 'NORMAL' THEN 3 
                    WHEN 'LOW' THEN 4 
                    ELSE 5 
                END,
                created_at ASC 
            LIMIT :limit
        """.trimIndent()

        return executeQuery(sql, mapOf("limit" to limit))
    }

    // Get overdue jobs
    suspend fun getOverdueJobs(): List<ProductionJob> {
        val sql = """
            SELECT * FROM $tableName 
            WHERE status NOT IN ('COMPLETED', 'CANCELLED') 
              AND estimated_completion_at IS NOT NULL 
              AND estimated_completion_at < :now 
            ORDER BY estimated_completion_at ASC
        """.trimIndent()

        return executeQuery(sql, mapOf("now" to OffsetDateTime.now()))
    }

    // Get production statistics
    suspend fun getProductionStats(): ProductionStats {
        val sql = """
            SELECT 
                COUNT(*) as total_jobs,
                COUNT(CASE WHEN status = 'QUEUED' THEN 1 END) as queued,
                COUNT(CASE WHEN status = 'IN_PROGRESS' THEN 1 END) as in_progress,
                COUNT(CASE WHEN status = 'PAUSED' THEN 1 END) as paused,
                COUNT(CASE WHEN status = 'COMPLETED' THEN 1 END) as completed,
                COUNT(CASE WHEN status = 'CANCELLED' THEN 1 END) as cancelled,
                COUNT(CASE WHEN priority = 'URGENT' AND status NOT IN ('COMPLETED', 'CANCELLED') THEN 1 END) as urgent,
                COUNT(CASE WHEN priority = 'HIGH' AND status NOT IN ('COMPLETED', 'CANCELLED') THEN 1 END) as high_priority
            FROM $tableName
        """.trimIndent()

        return connectionFactory.useConnection {
            createStatement(sql)
                .execute()
                .awaitSingle()
                .map { row, _ ->
                    ProductionStats(
                        totalJobs = (row.get("total_jobs", Long::class.java) ?: 0L).toInt(),
                        queued = (row.get("queued", Long::class.java) ?: 0L).toInt(),
                        inProgress = (row.get("in_progress", Long::class.java) ?: 0L).toInt(),
                        paused = (row.get("paused", Long::class.java) ?: 0L).toInt(),
                        completed = (row.get("completed", Long::class.java) ?: 0L).toInt(),
                        cancelled = (row.get("cancelled", Long::class.java) ?: 0L).toInt(),
                        urgent = (row.get("urgent", Long::class.java) ?: 0L).toInt(),
                        highPriority = (row.get("high_priority", Long::class.java) ?: 0L).toInt()
                    )
                }
                .awaitFirstOrNull() ?: ProductionStats(0, 0, 0, 0, 0, 0, 0, 0)
        }
    }

    // Get jobs by date range
    suspend fun findByDateRange(
        startDate: OffsetDateTime,
        endDate: OffsetDateTime,
        status: String? = null,
        offset: Int = 0,
        limit: Int = 50
    ): List<ProductionJob> {
        val statusFilter = if (status != null) "AND status = :status" else ""

        val sql = """
            SELECT * FROM $tableName 
            WHERE created_at BETWEEN :startDate AND :endDate 
            $statusFilter
            ORDER BY created_at DESC 
            LIMIT :limit OFFSET :offset
        """.trimIndent()

        val params = mutableMapOf(
            "startDate" to startDate,
            "endDate" to endDate,
            "limit" to limit,
            "offset" to offset.toLong()
        )

        if (status != null) {
            params["status"] = status
        }

        return executeQuery(sql, params)
    }

    // Get user's workload
    suspend fun getUserWorkload(assignedTo: String): UserWorkload {
        val sql = """
            SELECT 
                COUNT(*) as total_assigned,
                COUNT(CASE WHEN status = 'IN_PROGRESS' THEN 1 END) as currently_working,
                COUNT(CASE WHEN priority = 'URGENT' AND status NOT IN ('COMPLETED', 'CANCELLED') THEN 1 END) as urgent_tasks,
                COUNT(CASE WHEN status NOT IN ('COMPLETED', 'CANCELLED') 
                      AND estimated_completion_at IS NOT NULL 
                      AND estimated_completion_at < :now THEN 1 END) as overdue_tasks
            FROM $tableName 
            WHERE assigned_to = :assignedTo 
              AND status NOT IN ('COMPLETED', 'CANCELLED')
        """.trimIndent()

        return connectionFactory.useConnection {
            createStatement(sql)
                .bind("assignedTo", assignedTo)
                .bind("now", OffsetDateTime.now())
                .execute()
                .awaitSingle()
                .map { row, _ ->
                    UserWorkload(
                        totalAssigned = (row.get("total_assigned", Long::class.java) ?: 0L).toInt(),
                        currentlyWorking = (row.get("currently_working", Long::class.java) ?: 0L).toInt(),
                        urgentTasks = (row.get("urgent_tasks", Long::class.java) ?: 0L).toInt(),
                        overdueTasks = (row.get("overdue_tasks", Long::class.java) ?: 0L).toInt()
                    )
                }
                .awaitFirstOrNull() ?: UserWorkload(0, 0, 0, 0)
        }
    }

    private fun validateProductionJob(job: ProductionJob) {
        if (job.quantity <= 0) {
            throw IllegalArgumentException("Production quantity must be greater than zero")
        }

        val validStatuses = listOf("QUEUED", "IN_PROGRESS", "PAUSED", "COMPLETED", "CANCELLED")
        if (job.status !in validStatuses) {
            throw IllegalArgumentException("Invalid job status: ${job.status}")
        }

        val validPriorities = listOf("LOW", "NORMAL", "HIGH", "URGENT")
        if (job.priority !in validPriorities) {
            throw IllegalArgumentException("Invalid priority: ${job.priority}")
        }
    }
}

@Serializable
data class ProductionStats(
    val totalJobs: Int,
    val queued: Int,
    val inProgress: Int,
    val paused: Int,
    val completed: Int,
    val cancelled: Int,
    val urgent: Int,
    val highPriority: Int
)

@Serializable
data class UserWorkload(
    val totalAssigned: Int,
    val currentlyWorking: Int,
    val urgentTasks: Int,
    val overdueTasks: Int
)