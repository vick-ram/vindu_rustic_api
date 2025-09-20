package org.example.domain.repo

import org.example.domain.models.Category

interface CategoryRepository: CrudRepository<Category, String> {
    suspend fun findBySlug(slug: String): Category?
}


class CachedCategory(
    private val delegate: CategoryRepository,
    private val cache: CrudRepository<Category, String>
): CategoryRepository {
    override suspend fun findBySlug(slug: String): Category? {
        return delegate.findBySlug(slug)
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