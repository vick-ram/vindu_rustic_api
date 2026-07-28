package org.example.data.repo

import io.r2dbc.spi.ConnectionFactory
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import org.example.data.mappers.CustomProductQuoteMapper
import org.example.di.Component
import org.example.di.Inject
import org.example.domain.models.customization.CustomProductQuote
import java.time.OffsetDateTime

@Component
class CustomProductQuoteRepository @Inject constructor(
    connectionFactory: ConnectionFactory,
    customProductQuoteMapper: CustomProductQuoteMapper
) : CrudRepository<CustomProductQuote, String>(
    connectionFactory = connectionFactory,
    tableName = "custom_product_quotes",
    mapper = customProductQuoteMapper
) {
    override val generatedColumns = listOf("id", "created_at", "updated_at")

    // Get quotes for a request
    suspend fun findByRequestId(requestId: String): List<CustomProductQuote> {
        val sql = """
            SELECT * FROM $tableName 
            WHERE request_id = :requestId 
            ORDER BY created_at DESC
        """.trimIndent()

        return executeQuery(sql, mapOf("requestId" to requestId))
    }

    // Get latest quote for a request
    suspend fun getLatestQuote(requestId: String): CustomProductQuote? {
        val sql = """
            SELECT * FROM $tableName 
            WHERE request_id = :requestId 
            ORDER BY created_at DESC 
            LIMIT 1
        """.trimIndent()

        return connectionFactory.useConnection {
            createNamedStatement(sql, mapOf("requestId" to requestId))
                .execute()
                .awaitSingle()
                .map(rowMapper)
                .awaitFirstOrNull()
        }
    }

    // Get quotes by status
    suspend fun findByStatus(
        status: String,
        offset: Int = 0,
        limit: Int = 20
    ): List<CustomProductQuote> {
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

    // Get valid quotes (not expired)
    suspend fun getValidQuotes(requestId: String): List<CustomProductQuote> {
        val sql = """
            SELECT * FROM $tableName 
            WHERE request_id = :requestId 
              AND status IN ('sent', 'viewed') 
              AND (valid_until IS NULL OR valid_until > :now) 
            ORDER BY created_at DESC
        """.trimIndent()

        return executeQuery(
            sql, mapOf(
                "requestId" to requestId,
                "now" to OffsetDateTime.now()
            )
        )
    }

    // Accept a quote
    suspend fun acceptQuote(id: String): CustomProductQuote? {
        val sql = """
            UPDATE $tableName 
            SET status = 'accepted', 
                updated_at = :updatedAt 
            WHERE id = :id 
              AND status IN ('sent', 'viewed')
              AND (valid_until IS NULL OR valid_until > :now)
            RETURNING *
        """.trimIndent()

        return connectionFactory.withTransaction { connection ->
            connection.createNamedStatement(
                sql,
                mapOf("id" to id, "updatedAt" to OffsetDateTime.now(), "now" to OffsetDateTime.now())
            )
                .execute()
                .awaitSingle()
                .map(rowMapper)
                .awaitFirstOrNull()
        }
    }

    // Reject a quote
    suspend fun rejectQuote(id: String, reason: String? = null): CustomProductQuote? {
        val sql = """
            UPDATE $tableName 
            SET status = 'rejected', 
                updated_at = :updatedAt 
            WHERE id = :id 
            RETURNING *
        """.trimIndent()

        return connectionFactory.withTransaction { connection ->
            connection.createNamedStatement(
                sql, mapOf(
                    "id" to id, "updatedAt" to OffsetDateTime.now()
                )
            )
                .execute()
                .awaitSingle()
                .map(rowMapper)
                .awaitFirstOrNull()
        }
    }

    // Mark quote as viewed
    suspend fun markAsViewed(id: String): CustomProductQuote? {
        val sql = """
            UPDATE $tableName 
            SET status = CASE 
                    WHEN status = 'sent' THEN 'viewed' 
                    ELSE status 
                END,
                updated_at = :updatedAt 
            WHERE id = :id 
            RETURNING *
        """.trimIndent()

        return connectionFactory.withTransaction { connection ->
            connection.createNamedStatement(sql, mapOf("id" to id, "updatedAt" to OffsetDateTime.now()))
                .execute()
                .awaitSingle()
                .map(rowMapper)
                .awaitFirstOrNull()
        }
    }

    // Expire old quotes
    suspend fun expireOldQuotes(): Int {
        val sql = """
            UPDATE $tableName 
            SET status = 'expired', 
                updated_at = :updatedAt 
            WHERE status IN ('sent', 'viewed') 
              AND valid_until IS NOT NULL 
              AND valid_until <= :now
        """.trimIndent()

        return connectionFactory.withTransaction { connection ->
            connection.createNamedStatement(
                sql,
                mapOf("updatedAt" to OffsetDateTime.now(), "now" to OffsetDateTime.now())
            )
                .execute()
                .awaitSingle()
                .rowsUpdated
                .awaitSingle()
                .toInt()
        }
    }

    // Get quotes by creator
    suspend fun findByCreator(
        createdBy: String,
        offset: Int = 0,
        limit: Int = 20
    ): List<CustomProductQuote> {
        val sql = """
            SELECT * FROM $tableName 
            WHERE createdBy = :createdBy 
            ORDER BY created_at DESC 
            LIMIT :limit OFFSET :offset
        """.trimIndent()

        return executeQuery(
            sql, mapOf(
                "createdBy" to createdBy,
                "limit" to limit,
                "offset" to offset.toLong()
            )
        )
    }
}