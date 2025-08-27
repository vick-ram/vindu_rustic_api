package org.example.domain.repo

import org.example.domain.models.Product

interface ProductRepository : CrudRepository<Product, String> {
    suspend fun findBySku(sku: String): Product?
    suspend fun findByCategory(categoryId: String, offset: Int = 0, limit: Int = 50): List<Product>
    suspend fun searchProducts(query: String, offset: Int = 0, limit: Int = 50): List<Product>
    suspend fun updateProductStock(productId: String, available: Int): Product?
    suspend fun getProductsWithLowStock(): List<Product>
    suspend fun markProductViewed(productId: String): Product?

}