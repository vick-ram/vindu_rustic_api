package org.example.domain.repo

import org.example.domain.models.catalog.Category

interface CategoryRepository: CrudRepository<Category, String> {
    suspend fun findBySlug(slug: String): Category?
    suspend fun findChildren(parentId: String): List<Category>
    suspend fun findActive(): List<Category>
    suspend fun searchCategories(query: String, offset: Int, limit: Int): List<Category>
}