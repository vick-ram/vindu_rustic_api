package org.example.data.repo

import io.r2dbc.spi.ConnectionFactory
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import org.example.data.mappers.ProductionStageMapper
import org.example.di.Component
import org.example.di.Inject
import org.example.domain.models.production.ProductionStage
import java.time.OffsetDateTime

@Component
class ProductionStageRepository @Inject constructor(
    connectionFactory: ConnectionFactory,
    productionStageMapper: ProductionStageMapper
) : CrudRepository<ProductionStage, String>(
    connectionFactory = connectionFactory,
    tableName = "production_stages",
    mapper = productionStageMapper
) {
    override val generatedColumns = listOf("id")

    /**
     * Find all production stages for a given job ordered by execution lifecycle.
     */
    suspend fun findByJobId(jobId: String): List<ProductionStage> {
        val sql = """
            SELECT * FROM $tableName 
            WHERE job_id = :jobId 
            ORDER BY started_at ASC NULLS LAST, id ASC
        """.trimIndent()

        return executeQuery(sql, mapOf("jobId" to jobId))
    }

    /**
     * Find stages filtered by status.
     */
    suspend fun findByStatus(status: String, limit: Int = 50, offset: Int = 0): List<ProductionStage> {
        val sql = """
            SELECT * FROM $tableName 
            WHERE status = :status 
            ORDER BY started_at DESC NULLS LAST 
            LIMIT :limit OFFSET :offset
        """.trimIndent()

        return executeQuery(
            sql,
            mapOf(
                "status" to status,
                "limit" to limit,
                "offset" to offset.toLong()
            )
        )
    }

    /**
     * Start a stage lifecycle transition to 'in_progress'.
     */
    suspend fun startStage(id: String, startedAt: OffsetDateTime = OffsetDateTime.now()): ProductionStage? {
        val sql = """
            UPDATE $tableName 
            SET status = 'in_progress', 
                started_at = :startedAt 
            WHERE id = :id 
            RETURNING *
        """.trimIndent()

        return connectionFactory.withTransaction { connection ->
            connection.createNamedStatement(sql, mapOf("id" to id, "startedAt" to startedAt))
                .execute()
                .awaitSingle()
                .map(rowMapper)
                .awaitFirstOrNull()
        }
    }

    /**
     * Complete a stage execution transition to 'completed'.
     */
    suspend fun completeStage(id: String, completedAt: OffsetDateTime = OffsetDateTime.now()): ProductionStage? {
        val sql = """
            UPDATE $tableName 
            SET status = 'completed', 
                completed_at = :completedAt 
            WHERE id = :id 
            RETURNING *
        """.trimIndent()

        return connectionFactory.withTransaction { connection ->
            connection.createNamedStatement(sql, mapOf("id" to id, "completedAt" to completedAt))
                .execute()
                .awaitSingle()
                .map(rowMapper)
                .awaitFirstOrNull()
        }
    }

    /**
     * Mark a stage as failed.
     */
    suspend fun failStage(id: String, failedAt: OffsetDateTime = OffsetDateTime.now()): ProductionStage? {
        val sql = """
            UPDATE $tableName 
            SET status = 'failed', 
                completed_at = :failedAt 
            WHERE id = :id 
            RETURNING *
        """.trimIndent()

        return connectionFactory.withTransaction { connection ->
            connection.createNamedStatement(sql, mapOf("id" to id, "failedAt" to failedAt))
                .execute()
                .awaitSingle()
                .map(rowMapper)
                .awaitFirstOrNull()
        }
    }

    /**
     * Fetch active stages currently in progress for a job.
     */
    suspend fun findActiveStagesForJob(jobId: String): List<ProductionStage> {
        val sql = """
            SELECT * FROM $tableName 
            WHERE job_id = :jobId AND status = 'in_progress' 
            ORDER BY started_at ASC
        """.trimIndent()

        return executeQuery(sql, mapOf("jobId" to jobId))
    }
}