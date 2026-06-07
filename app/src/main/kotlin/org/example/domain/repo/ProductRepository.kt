package org.example.domain.repo

import io.ktor.http.content.*
import org.example.domain.models.catalog.Product

interface ProductRepository : CrudRepository<Product, String> {
    suspend fun findByCategory(categoryId: String, offset: Int = 0, limit: Int = 50): List<Product>
    suspend fun searchProducts(query: String, offset: Int = 0, limit: Int = 50): List<Product>

    suspend fun findBySlug(slug: String): Product?
    suspend fun findFeatured(): List<Product>
    suspend fun updateStatus(productId: String, status: String): Boolean
    suspend fun softDelete(productId: String): Boolean
}