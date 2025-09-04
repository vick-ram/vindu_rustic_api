package org.example.domain.repo

import io.ktor.http.content.PartData
import org.example.domain.models.CreateProductRequest
import org.example.domain.models.Product

interface ProductRepository : CrudRepository<Product, String> {
    suspend fun createProduct(request: CreateProductRequest, mediaFiles: List<PartData.FileItem>?): Product?
    suspend fun findBySku(sku: String): Product?
    suspend fun findByCategory(categoryId: String, offset: Int = 0, limit: Int = 50): List<Product>
    suspend fun searchProducts(query: String, offset: Int = 0, limit: Int = 50): List<Product>
    suspend fun updateProductStock(productId: String, available: Int): Product?
    suspend fun getProductsWithLowStock(): List<Product>
    suspend fun markProductViewed(productId: String): Product?
    suspend fun markProductAsFavorite(productId: String): Product?

}