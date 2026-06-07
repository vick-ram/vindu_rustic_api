package org.example.data.cache

import org.example.domain.models.catalog.Category
import org.example.domain.repo.CategoryRepository
import org.example.domain.repo.CrudRepository

class CachedCategoryRepository(
    private val delegate: CategoryRepository,
    private val cache: CrudRepository<Category, String>
): CategoryRepository {
    override suspend fun findBySlug(slug: String): Category? {
        return delegate.findBySlug(slug)
    }

    override suspend fun findChildren(parentId: String): List<Category> {
        return delegate.findChildren(parentId)
    }

    override suspend fun findActive(): List<Category> {
        return delegate.findActive()
    }

    override suspend fun searchCategories(
        query: String,
        offset: Int,
        limit: Int
    ): List<Category> {
        return delegate.searchCategories(query, offset, limit)
    }

    override suspend fun create(entity: Category): Category {
        return cache.create(entity)
    }

    override suspend fun read(id: String): Category? {
        return cache.read(id)
    }

    override suspend fun readAll(
        offset: Int,
        limit: Int,
        queryParams: Map<String, String>?
    ): List<Category> {
        return cache.readAll(offset, limit, queryParams)
    }

    override suspend fun update(
        id: String,
        entity: Category
    ): Category? {
        return cache.update(id, entity)
    }

    override suspend fun delete(id: String): Boolean {
        return cache.delete(id)
    }
}