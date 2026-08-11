package org.example.data.repo

import io.r2dbc.spi.ConnectionFactory
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import org.example.data.mappers.CategoryMapper
import org.example.di.Component
import org.example.di.Inject
import org.example.domain.models.catalog.Category

@Component
class CategoryRepository @Inject constructor(
    connectionFactory: ConnectionFactory,
    categoryMapper: CategoryMapper
) :
    CrudRepository<Category, String>(connectionFactory = connectionFactory, tableName = "categories", mapper = categoryMapper) {

    suspend fun findBySlug(slug: String): Category? {
        val sql = "SELECT * FROM categories WHERE slug = $1"
        return connectionFactory.useConnection {
            createStatement(sql)
                .bind("$1", slug)
                .execute()
                .awaitSingle()
                .map(rowMapper)
                .awaitFirstOrNull()
        }
    }

    suspend fun findChildren(parentId: String): List<Category>  {
        val sql = "SELECT * FROM categories WHERE parent_id = $1"
        return connectionFactory.useConnection {
            createStatement(sql)
                .bind("$1", parentId)
                .execute()
                .awaitSingle()
                .map(rowMapper)
                .asFlow()
                .toList()
        }
    }

    suspend fun findActive(): List<Category> {
        return connectionFactory.useConnection {
            createStatement("SELECT * FROM categories WHERE is_active = $1")
                .bind("$1", true)
                .execute()
                .awaitSingle()
                .map(rowMapper)
                .asFlow()
                .toList()
        }
    }

    suspend fun searchCategories(query: String, offset: Int, limit: Int): List<Category>  {
        return search(query = query, limit = limit, offset = offset).toList()
    }
}