package org.example.data.repo

import io.r2dbc.spi.ConnectionFactory
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import org.example.data.mappers.TagMapper
import org.example.di.Component
import org.example.di.Inject
import org.example.domain.models.catalog.Tag

@Component
class TagRepository @Inject constructor(connectionFactory: ConnectionFactory, tagMapper: TagMapper) :
    CrudRepository<Tag, String>(connectionFactory = connectionFactory, tableName = "tags", mapper = tagMapper) {

    override val generatedColumns: List<String> = listOf("id", "created_at")

    suspend fun findBySlug(slug: String): Tag? {
        val sql = "SELECT * FROM $tableName WHERE slug = :slug"

        return connectionFactory.useConnection {
            createNamedStatement(sql, mapOf("slug" to slug))
                .execute()
                .awaitSingle()
                .map(rowMapper)
                .awaitFirstOrNull()
        }
    }

    suspend fun bulkCreate(tags: List<Tag>): List<Tag> {
        if (tags.isEmpty()) return emptyList()

        val columns = listOf("name", "slug")
        val placeholders = List(tags.size) { index ->
            columns.joinToString(", ") { column -> ":${column}_$index" }
        }

        val sql = """
            INSERT INTO $tableName (${columns.joinToString(", ")})
            VALUES ${placeholders.joinToString(", ")}
            RETURNING *
        """.trimIndent()

        val params = mutableMapOf<String, Any>()
        tags.forEachIndexed { index, tag ->
            params["name_$index"] = tag.name
            params["slug_$index"] = tag.slug
        }

        return connectionFactory.withTransaction { connection ->
            connection.createNamedStatement(sql, params)
                .execute()
                .awaitSingle()
                .map(rowMapper)
                .asFlow()
                .toList()
        }
    }

    suspend fun findByName(name: String): Tag? {
        val sql = "SELECT * FROM $tableName WHERE LOWER(name) = LOWER(:name)"

        return connectionFactory.useConnection {
            createNamedStatement(sql, mapOf("name" to name))
                .execute()
                .awaitSingle()
                .map(rowMapper)
                .awaitFirstOrNull()
        }
    }

    suspend fun getMostUsedTags(limit: Int = 10): List<Tag> {
        val sql = """
            SELECT t.*, COUNT(pt.product_id) as usage_count
            FROM $tableName t
            LEFT JOIN product_tags pt ON t.id = pt.tag_id
            GROUP BY t.id
            ORDER BY usage_count DESC
            LIMIT :limit
        """.trimIndent()

        return executeQuery(sql, mapOf("limit" to limit))
    }

    suspend fun searchTags(query: String, offset: Int, limit: Int): List<Tag> {
        return search(query = query, offset = offset, limit = limit).toList()
    }

    suspend fun deleteUnusedTags(): Int {
        val sql = """
            DELETE FROM $tableName 
            WHERE id NOT IN (
                SELECT DISTINCT tag_id FROM product_tags
            )
        """.trimIndent()

        return connectionFactory.withTransaction { connection ->
            connection.createStatement(sql)
                .execute()
                .awaitSingle()
                .rowsUpdated
                .awaitSingle()
                .toInt()
        }
    }
}
