package org.example.data.repo

import io.r2dbc.spi.Connection
import io.r2dbc.spi.ConnectionFactory
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.example.data.mappers.ProductionUpdateMapper
import org.example.di.Component
import org.example.di.Inject
import org.example.domain.models.production.ProductionUpdate
import org.koin.core.annotation.Single
import java.time.OffsetDateTime

@Component
class ProductionUpdateRepository @Inject constructor(
    connectionFactory: ConnectionFactory,
    productionUpdateMapper: ProductionUpdateMapper
) : CrudRepository<ProductionUpdate, String>(
    connectionFactory = connectionFactory,
    tableName = "production_updates",
    mapper = productionUpdateMapper
) {
    override val generatedColumns = listOf("id", "created_at")

    // Get updates for a job
    suspend fun findByJobId(
        jobId: String,
        offset: Int = 0,
        limit: Int = 50
    ): List<ProductionUpdate> {
        val sql = """
            SELECT * FROM $tableName 
            WHERE job_id = :jobId 
            ORDER BY created_at DESC 
            LIMIT :limit OFFSET :offset
        """.trimIndent()

        return executeQuery(
            sql, mapOf(
                "jobId" to jobId,
                "limit" to limit,
                "offset" to offset.toLong()
            )
        )
    }

    // Get updates by status
    suspend fun findByStatus(
        status: String,
        offset: Int = 0,
        limit: Int = 20
    ): List<ProductionUpdate> {
        val sql = """
            SELECT * FROM $tableName 
            WHERE status = :status 
            ORDER BY created_at DESC 
            LIMIT :limit OFFSET :offset
        """.trimIndent()

        return executeQuery(
            sql, mapOf(
                "status" to status,
                "limit" to limit,
                "offset" to offset.toLong()
            )
        )
    }

    // Get updates posted by a user
    suspend fun findByPostedBy(
        postedBy: String,
        offset: Int = 0,
        limit: Int = 20
    ): List<ProductionUpdate> {
        val sql = """
            SELECT * FROM $tableName 
            WHERE posted_by = :postedBy 
            ORDER BY created_at DESC 
            LIMIT :limit OFFSET :offset
        """.trimIndent()

        return executeQuery(
            sql, mapOf(
                "postedBy" to postedBy,
                "limit" to limit,
                "offset" to offset.toLong()
            )
        )
    }

    // Get latest update for a job
    suspend fun getLatestUpdate(jobId: String): ProductionUpdate? {
        val sql = """
            SELECT * FROM $tableName 
            WHERE job_id = :jobId 
            ORDER BY created_at DESC 
            LIMIT 1
        """.trimIndent()

        return connectionFactory.useConnection {
            createNamedStatement(sql, mapOf("jobId" to jobId))
                .execute()
                .awaitSingle()
                .map(rowMapper)
                .awaitFirstOrNull()
        }

    }

    // Get updates by stage name
    suspend fun findByStage(
        jobId: String,
        stageName: String
    ): List<ProductionUpdate> {
        val sql = """
            SELECT * FROM $tableName 
            WHERE job_id = :jobId 
              AND stage_name = :stageName 
            ORDER BY created_at DESC
        """.trimIndent()

        return executeQuery(
            sql, mapOf(
                "jobId" to jobId,
                "stageName" to stageName
            )
        )
    }

    // Get updates with images
    suspend fun findWithImages(
        jobId: String
    ): List<ProductionUpdate> {
        val sql = """
            SELECT * FROM $tableName 
            WHERE job_id = :jobId 
              AND image_url IS NOT NULL 
            ORDER BY created_at DESC
        """.trimIndent()

        return executeQuery(sql, mapOf("jobId" to jobId))
    }

    // Get updates for a date range
    suspend fun findByDateRange(
        startDate: OffsetDateTime,
        endDate: OffsetDateTime,
        jobId: String? = null,
        offset: Int = 0,
        limit: Int = 50
    ): List<ProductionUpdate> {
        val jobFilter = if (jobId != null) "AND job_id = :jobId" else ""

        val sql = """
            SELECT * FROM $tableName 
            WHERE created_at BETWEEN :startDate AND :endDate 
            $jobFilter
            ORDER BY created_at DESC 
            LIMIT :limit OFFSET :offset
        """.trimIndent()

        val params = mutableMapOf(
            "startDate" to startDate,
            "endDate" to endDate,
            "limit" to limit,
            "offset" to offset.toLong()
        )

        if (jobId != null) {
            params["jobId"] = jobId
        }

        return executeQuery(sql, params)
    }

    // Get update timeline for a job
    suspend fun getJobTimeline(jobId: String): List<JobTimelineEvent> {
        val sql = """
            SELECT 
                id,
                job_id,
                status,
                stage_name,
                description,
                image_url,
                posted_by,
                created_at,
                LAG(status) OVER (ORDER BY created_at) as previous_status,
                LAG(stage_name) OVER (ORDER BY created_at) as previous_stage
            FROM $tableName 
            WHERE job_id = :jobId 
            ORDER BY created_at ASC
        """.trimIndent()

        return connectionFactory.useConnection {
            createNamedStatement(sql, mapOf("jobId" to jobId))
                .execute()
                .awaitSingle()
                .map { row, rowMetadata ->
                    val update = rowMapper.apply(row, rowMetadata)
                    val previousStatus = row.get("previous_status", String::class.java)
                    val previousStage = row.get("previous_stage", String::class.java)

                    JobTimelineEvent(
                        update = update,
                        previousStatus = previousStatus,
                        previousStage = previousStage,
                        isStatusChange = previousStatus != null && previousStatus != update.status,
                        isStageChange = previousStage != null && previousStage != update.stageName
                    )
                }
                .asFlow()
                .toList()
        }
    }

    // Create update with validation
    override suspend fun create(model: ProductionUpdate, connection: Connection?): ProductionUpdate {
        validateProductionUpdate(model)
        return super.create(model, connection)
    }

    // Bulk create updates
    suspend fun bulkCreate(updates: List<ProductionUpdate>): List<ProductionUpdate> {
        if (updates.isEmpty()) return emptyList()

        updates.forEach { validateProductionUpdate(it) }

        val columns = listOf("job_id", "status", "stage_name", "description", "image_url", "posted_by")
        val placeholders = List(updates.size) { index ->
            "(${columns.joinToString(", ") { ":${it}_$index" }})"
        }

        val sql = """
            INSERT INTO $tableName (${columns.joinToString(", ")})
            VALUES ${placeholders.joinToString(", ")}
            RETURNING *
        """.trimIndent()

        val params = mutableMapOf<String, Any>()

        updates.forEachIndexed { index, update ->
            params["job_id_$index"] = update.jobId
            params["status_$index"] = update.status
            params["stage_name_$index"] = update.stageName ?: String::class.java
            params["description_$index"] = update.description ?: String::class.java
            params["image_url_$index"] = update.imageUrl ?: String::class.java
            params["posted_by_$index"] = update.postedBy
        }

        return connectionFactory.withTransaction { connection ->
            connection.createStatement(sql)
                .execute()
                .awaitSingle()
                .map(rowMapper)
                .asFlow()
                .toList()
        }
    }

    // Get update statistics
    suspend fun getUpdateStats(jobId: String): UpdateStats {
        val sql = """
            SELECT 
                COUNT(*) as total_updates,
                COUNT(DISTINCT posted_by) as unique_posters,
                COUNT(CASE WHEN image_url IS NOT NULL THEN 1 END) as updates_with_images,
                COUNT(DISTINCT stage_name) as stages_updated,
                MIN(created_at) as first_update,
                MAX(created_at) as last_update
            FROM $tableName 
            WHERE job_id = :jobId
        """.trimIndent()

        return connectionFactory.useConnection {
            createNamedStatement(sql, mapOf("jobId" to jobId))
                .execute()
                .awaitSingle()
                .map { row, _ ->
                    UpdateStats(
                        totalUpdates = (row.get("total_updates", Long::class.java) ?: 0L).toInt(),
                        uniquePosters = (row.get("unique_posters", Long::class.java) ?: 0L).toInt(),
                        updatesWithImages = (row.get("updates_with_images", Long::class.java) ?: 0L).toInt(),
                        stagesUpdated = (row.get("stages_updated", Long::class.java) ?: 0L).toInt(),
                        firstUpdate = row.get("first_update", OffsetDateTime::class.java),
                        lastUpdate = row.get("last_update", OffsetDateTime::class.java)
                    )
                }
                .awaitFirstOrNull() ?: UpdateStats(0, 0, 0, 0, null, null)
        }
    }

    private fun validateProductionUpdate(update: ProductionUpdate) {
        if (update.jobId.isBlank()) {
            throw IllegalArgumentException("Job ID cannot be blank")
        }

        if (update.status.isBlank()) {
            throw IllegalArgumentException("Status cannot be blank")
        }

        if (update.postedBy.isBlank()) {
            throw IllegalArgumentException("Posted by cannot be blank")
        }
    }
}

@Serializable
data class JobTimelineEvent(
    val update: ProductionUpdate,
    val previousStatus: String?,
    val previousStage: String?,
    val isStatusChange: Boolean,
    val isStageChange: Boolean
)

@Serializable
data class UpdateStats(
    val totalUpdates: Int,
    val uniquePosters: Int,
    val updatesWithImages: Int,
    val stagesUpdated: Int,
    @Contextual
    val firstUpdate: OffsetDateTime?,
    @Contextual
    val lastUpdate: OffsetDateTime?
)