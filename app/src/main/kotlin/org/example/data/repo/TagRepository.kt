package org.example.data.repo

import io.r2dbc.spi.ConnectionFactory
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import org.example.data.mappers.TagMapper
import org.example.domain.models.catalog.Tag

class TagRepository(connectionFactory: ConnectionFactory, tagMapper: TagMapper) :
    CrudRepository<Tag, String>(connectionFactory = connectionFactory, tableName = "tags", mapper = tagMapper) {

    override val generatedColumns: List<String> = listOf("id", "created_at")

    suspend fun findBySlug(slug: String): Tag? {
        val sql = "SELECT * FROM $tableName WHERE slug = :slug"

        return connectionFactory.useConnection {
            createStatement(sql)
                .bind("slug", slug)
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

        return connectionFactory.withTransaction { connection ->
            val statement = connection.createStatement(sql)
            tags.forEachIndexed { index, tag ->
                statement.bind("name_$index", tag.name)
                statement.bind("slug_$index", tag.slug)
            }

            statement.execute()
                .awaitSingle()
                .map(rowMapper)
                .asFlow()
                .toList()
        }
    }

    suspend fun findByName(name: String): Tag? {
        val sql = "SELECT * FROM $tableName WHERE LOWER(name) = LOWER(:name)"

        return connectionFactory.useConnection {
            createStatement(sql)
                .bind("name", name)
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
        return search(query = query, offset = offset, limit = limit)
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
                .awaitSingle() as Int
        }
    }
}
