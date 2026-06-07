package org.example.data.cache

import org.example.domain.models.catalog.Product
import org.example.domain.repo.CrudRepository
import org.example.domain.repo.ProductRepository

class CachedProductRepository(
    private val delegate: ProductRepository,
    private val cache: CrudRepository<Product, String>
) : ProductRepository {
    override suspend fun create(entity: Product): Product = cache.create(entity)
    override suspend fun read(id: String): Product? = cache.read(id)
    override suspend fun readAll(offset: Int, limit: Int, queryParams: Map<String, String>?): List<Product> =
        cache.readAll(offset, limit, queryParams)

    override suspend fun update(id: String, entity: Product): Product? = cache.update(id, entity)
    override suspend fun delete(id: String): Boolean = cache.delete(id)

    override suspend fun findBySlug(slug: String): Product? = delegate.findBySlug(slug)
    override suspend fun findByCategory(categoryId: String, offset: Int, limit: Int): List<Product> =
        delegate.findByCategory(categoryId, offset, limit)

    override suspend fun findFeatured(): List<Product> = delegate.findFeatured()
    override suspend fun searchProducts(query: String, offset: Int, limit: Int): List<Product> =
        delegate.searchProducts(query, offset, limit)

    override suspend fun updateStatus(productId: String, status: String): Boolean =
        delegate.updateStatus(productId, status)

    override suspend fun softDelete(productId: String): Boolean = delegate.softDelete(productId)
}