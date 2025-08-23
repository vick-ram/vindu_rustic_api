package org.example.domain.repo

import org.example.domain.models.Category

interface CategoryRepository: CrudRepository<Category, String> {
    suspend fun findBySlug(slug: String): Category?
}