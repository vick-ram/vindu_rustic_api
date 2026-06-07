package org.example.services

import org.example.domain.models.catalog.Category
import org.example.domain.repo.CategoryRepository

class CategoryService(private val categoryRepository: CategoryRepository) {
    suspend fun createCategory(category: Category): Category? {
        return categoryRepository.create(category)
    }

    suspend fun getCategoryBySlug(slug: String): Category? {
        return categoryRepository.findBySlug(slug)
    }

    suspend fun getCategory(id: String): Category? {
        return categoryRepository.read(id)
    }

    suspend fun getCategories(offset: Int, limit: Int, queryParams: Map<String, String>?): List<Category> {
        return categoryRepository.readAll(offset, limit, queryParams)
    }

    suspend fun updateCategory(id: String, category: Category): Category? {
        return categoryRepository.update(id, category)
    }

    suspend fun deleteCategory(id: String): Boolean {
        return categoryRepository.delete(id)
    }
}