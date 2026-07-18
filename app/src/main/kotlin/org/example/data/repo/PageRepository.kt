package org.example.data.repo

import io.r2dbc.spi.ConnectionFactory
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import org.example.data.mappers.PageMapper
import org.example.di.Component
import org.example.di.Inject
import org.example.domain.models.content.Page
import org.koin.core.annotation.Single
import java.time.OffsetDateTime

@Component
class PageRepository @Inject constructor(
    connectionFactory: ConnectionFactory,
    pageMapper: PageMapper
) : CrudRepository<Page, String>(
    connectionFactory = connectionFactory,
    tableName = "pages",
    idColumn = "id",
    mapper = pageMapper
) {
    override val generatedColumns = listOf("id", "created_at", "updated_at")

    // Find page by slug
    suspend fun findBySlug(slug: String): Page? {
        val sql = "SELECT * FROM $tableName WHERE slug = :slug AND status = 'published'"

        return connectionFactory.useConnection {
            createStatement(sql)
                .bind("slug", slug)
                .execute()
                .awaitSingle()
                .map(rowMapper)
                .awaitFirstOrNull()
        }
    }

    // Find pages by status
    suspend fun findByStatus(
        status: String,
        offset: Int = 0,
        limit: Int = 20
    ): List<Page> {
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

    // Search pages by title or content
    suspend fun search(
        query: String,
        offset: Int = 0,
        limit: Int = 20
    ): List<Page> {
        val sql = """
            SELECT * FROM $tableName 
            WHERE title ILIKE :query 
               OR content ILIKE :query 
            ORDER BY 
                CASE WHEN status = 'published' THEN 0 ELSE 1 END,
                created_at DESC 
            LIMIT :limit OFFSET :offset
        """.trimIndent()

        return executeQuery(sql, mapOf(
            "query" to "%$query%",
            "limit" to limit,
            "offset" to offset.toLong()
        ))
    }

    // Publish a page
    suspend fun publish(id: String): Page? {
        val sql = """
            UPDATE $tableName 
            SET status = 'published', 
                published_at = :publishedAt, 
                updated_at = :updatedAt 
            WHERE id = :id 
            RETURNING *
        """.trimIndent()

        return connectionFactory.withTransaction { connection ->
            val now = OffsetDateTime.now()
            connection.createStatement(sql)
                .bind("id", id)
                .bind("publishedAt", now)
                .bind("updatedAt", now)
                .execute()
                .awaitSingle()
                .map(rowMapper)
                .awaitFirstOrNull()
        }
    }

    // Unpublish a page
    suspend fun unpublish(id: String): Page? {
        val sql = """
            UPDATE $tableName 
            SET status = 'draft', 
                published_at = NULL, 
                updated_at = :updatedAt 
            WHERE id = :id 
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

    // Check if slug exists (useful for validation)
    suspend fun slugExists(slug: String, excludeId: String? = null): Boolean {
        val sql = if (excludeId != null) {
            "SELECT COUNT(*) as count FROM $tableName WHERE slug = :slug AND id != :excludeId"
        } else {
            "SELECT COUNT(*) as count FROM $tableName WHERE slug = :slug"
        }

        return connectionFactory.useConnection {
            val statement = createStatement(sql).bind("slug", slug)
            if (excludeId != null) {
                statement.bind("excludeId", excludeId)
            }
            statement.execute()
                .awaitSingle()
                .map { row, _ -> row.get("count", Long::class.java)!! > 0 }
                .awaitFirstOrNull() ?: false
        }
    }

    // Get recently published pages
    suspend fun getRecentlyPublished(limit: Int = 5): List<Page> {
        val sql = """
            SELECT * FROM $tableName 
            WHERE status = 'published' 
            ORDER BY published_at DESC NULLS LAST 
            LIMIT :limit
        """.trimIndent()

        return executeQuery(sql, mapOf("limit" to limit))
    }

    // Bulk update status
    suspend fun bulkUpdateStatus(ids: List<String>, status: String): Int {
        if (ids.isEmpty()) return 0

        val placeholders = List(ids.size) { index -> ":id$index" }
        val sql = """
            UPDATE $tableName 
            SET status = :status, 
                updated_at = :updatedAt,
                published_at = CASE 
                    WHEN :status = 'published' AND published_at IS NULL THEN :publishedAt 
                    ELSE published_at 
                END
            WHERE id IN (${placeholders.joinToString(", ")})
        """.trimIndent()

        return connectionFactory.withTransaction { connection ->
            val statement = connection.createStatement(sql)
                .bind("status", status)
                .bind("updatedAt", OffsetDateTime.now())
                .bind("publishedAt", OffsetDateTime.now())

            ids.forEachIndexed { index, id ->
                statement.bind("id$index", id)
            }

            statement.execute()
                .awaitSingle()
                .rowsUpdated
                .awaitSingle()
                .toInt()
        }
    }
}