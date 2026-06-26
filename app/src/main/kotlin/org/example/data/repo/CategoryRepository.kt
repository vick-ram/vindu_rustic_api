package org.example.data.repo

import io.r2dbc.spi.ConnectionFactory
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import org.example.data.mappers.CategoryMapper
import org.example.domain.models.catalog.Category

class CategoryRepository(
    connectionFactory: ConnectionFactory,
    categoryMapper: CategoryMapper
) :
    CrudRepository<Category, String>(connectionFactory = connectionFactory, tableName = "categories", mapper = categoryMapper) {

    suspend fun findBySlug(slug: String): Category? {
        val sql = "SELECT * FROM categories WHERE slug = :slug"
        return connectionFactory.useConnection {
            createStatement(sql)
                .bind("slug", slug)
                .execute()
                .awaitSingle()
                .map(rowMapper)
                .awaitFirstOrNull()
        }
    }

    suspend fun findChildren(parentId: String): List<Category>  {
        val sql = "SELECT * FROM categories WHERE parent_id = :parentId"
        return connectionFactory.useConnection {
            createStatement(sql)
                .bind("parentId", parentId)
                .execute()
                .awaitSingle()
                .map(rowMapper)
                .asFlow()
                .toList()
        }
    }

    suspend fun findActive(): List<Category> {
        return connectionFactory.useConnection {
            createStatement("SELECT * FROM categories WHERE isActive = :isActive")
                .bind("isActive", true)
                .execute()
                .awaitSingle()
                .map(rowMapper)
                .asFlow()
                .toList()
        }
    }

    suspend fun searchCategories(query: String, offset: Int, limit: Int): List<Category>  {
        return search(query = query, limit = limit, offset = offset)
    }
}