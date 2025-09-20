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
class CachedProduct(
    private val delegate: ProductRepository,
    private val cache: CrudRepository<Product, String>
): ProductRepository {
    override suspend fun createProduct(
        request: CreateProductRequest,
        mediaFiles: List<PartData.FileItem>?
    ): Product? {
        return delegate.createProduct(request, mediaFiles)
    }

    override suspend fun findBySku(sku: String): Product? {
        return delegate.findBySku(sku)
    }

    override suspend fun findByCategory(
        categoryId: String,
        offset: Int,
        limit: Int
    ): List<Product> {
        return delegate.findByCategory(categoryId, offset, limit)
    }

    override suspend fun searchProducts(
        query: String,
        offset: Int,
        limit: Int
    ): List<Product> {
        return delegate.searchProducts(query, offset, limit)
    }

    override suspend fun updateProductStock(
        productId: String,
        available: Int
    ): Product? {
        return delegate.updateProductStock(productId, available)
    }

    override suspend fun getProductsWithLowStock(): List<Product> {
        return delegate.getProductsWithLowStock()
    }

    override suspend fun markProductViewed(productId: String): Product? {
        return delegate.markProductViewed(productId)
    }

    override suspend fun markProductAsFavorite(productId: String): Product? {
        return delegate.markProductAsFavorite(productId)
    }

    override suspend fun create(entity: Product): Product {
        return cache.create(entity)
    }

    override suspend fun read(id: String): Product? {
        return cache.read(id)
    }

    override suspend fun readAll(
        offset: Int,
        limit: Int,
        queryParams: Map<String, String>?
    ): List<Product> {
        return cache.readAll(offset, limit, queryParams)
    }

    override suspend fun update(
        id: String,
        entity: Product
    ): Product? {
        return cache.update(id, entity)
    }

    override suspend fun delete(id: String): Boolean {
        return cache.delete(id)
    }
}