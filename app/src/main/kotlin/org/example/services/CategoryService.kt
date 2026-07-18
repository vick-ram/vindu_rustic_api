package org.example.services

import org.example.data.cache.CategoryCache
import org.example.di.Injectable
import org.example.di.Qualifier
import org.example.domain.models.catalog.Category

@Injectable
class CategoryService(
    @Qualifier("categoryCache") private val categoryCache: CategoryCache
) {

    suspend fun createCategory(category: Category): Category {
        return categoryCache.create(category)
    }

    suspend fun getCategoryBySlug(slug: String): Category? {
        return categoryCache.findBySlug(slug)
    }

    suspend fun getSubCategories(parentId: String): List<Category> {
        return categoryCache.findChildren(parentId)
    }

    suspend fun getActiveCategories(): List<Category> {
        return categoryCache.findActive()
    }

    suspend fun getCategory(id: String): Category? {
        return categoryCache.read(id)
    }

    suspend fun getCategories(offset: Int, limit: Int, queryParams: Map<String, String>?): List<Category> {
        return categoryCache.readAll(offset, limit, queryParams)
    }

    suspend fun searchCategories(query: String, offset: Int, limit: Int): List<Category> {
        return categoryCache.searchCategories(query, offset, limit)
    }

    suspend fun updateCategory(id: String, category: Category): Category? {
        return categoryCache.update(id, category)
    }

    suspend fun deleteCategory(id: String): Boolean {
        return categoryCache.delete(id)
    }
}