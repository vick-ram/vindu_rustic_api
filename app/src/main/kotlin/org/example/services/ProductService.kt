package org.example.services

import org.example.data.cache.ProductCache
import org.example.data.repo.ProductRepository
import org.example.data.repo.ProductSearchResult
import org.example.data.repo.ProductStats
import org.example.data.repo.ProductWithStock
import org.example.di.Component
import org.example.di.Inject
import org.example.domain.models.catalog.Product
import org.slf4j.LoggerFactory
import java.time.OffsetDateTime

@Component
class ProductService @Inject constructor(
    private val productCache: ProductCache,
    private val productRepo: ProductRepository // For non-cacheable operations
) {
    private val logger = LoggerFactory.getLogger(ProductService::class.java)

    suspend fun getProduct(id: String): Product? {
        return productCache.read(id)
    }

    suspend fun getProductBySlug(slug: String, includeDeleted: Boolean = false): Product? {
        return productCache.findBySlug(slug, includeDeleted)
    }

    suspend fun getProductsByCategory(
        categoryId: String,
        status: String? = "published",
        offset: Int = 0,
        limit: Int = 20
    ): List<Product> {
        return productCache.findByCategoryId(categoryId, status, offset, limit)
    }

    suspend fun getProductsByStatus(
        status: String,
        offset: Int = 0,
        limit: Int = 20
    ): List<Product> {
        return productCache.findByStatus(status, offset, limit)
    }

    suspend fun getFeaturedProducts(offset: Int = 0, limit: Int = 20): List<Product> {
        return productCache.findFeatured(offset, limit)
    }

    suspend fun getCustomizableProducts(offset: Int = 0, limit: Int = 20): List<Product> {
        return productCache.findCustomizable(offset, limit)
    }

    suspend fun getProductsByBrand(
        brand: String,
        offset: Int = 0,
        limit: Int = 20
    ): List<Product> {
        return productCache.findByBrand(brand, offset, limit)
    }

    suspend fun getProductsByType(
        productType: String,
        offset: Int = 0,
        limit: Int = 20
    ): List<Product> {
        return productCache.findByProductType(productType, offset, limit)
    }

    suspend fun searchProducts(
        query: String,
        offset: Int = 0,
        limit: Int = 20
    ): List<Product> {
        return productCache.searchProduct(query, offset, limit)
    }

    suspend fun fullTextSearchProducts(
        query: String,
        offset: Int = 0,
        limit: Int = 20
    ): List<ProductSearchResult> {
        return productCache.fullTextSearch(query, offset, limit)
    }

    suspend fun getRelatedProducts(
        productId: String,
        limit: Int = 10
    ): List<Product> {
        return productCache.findRelated(productId, limit)
    }

    suspend fun getProductStats(): ProductStats {
        return productCache.getProductStats()
    }

    suspend fun getProductsByDateRange(
        startDate: OffsetDateTime,
        endDate: OffsetDateTime,
        status: String? = null,
        offset: Int = 0,
        limit: Int = 50
    ): List<Product> {
        return productCache.findByDateRange(startDate, endDate, status, offset, limit)
    }

    suspend fun getLatestProducts(
        limit: Int = 10,
        status: String = "published"
    ): List<Product> {
        return productCache.getLatest(limit, status)
    }

    suspend fun getLowStockProducts(
        warehouseId: String? = null,
        threshold: Int = 10,
        limit: Int = 20
    ): List<ProductWithStock> {
        return productCache.getLowStockProducts(warehouseId, threshold, limit)
    }

    suspend fun createProduct(product: Product): Product {

        val created = productRepo.create(product)
        productCache.putInCache(created.id, created)
        logger.info("Product created with ID: ${created.id}")
        return created
    }

    suspend fun updateProduct(id: String, updates: Product): Product? {

        val existing = productRepo.read(id) ?: run {
            logger.warn("Product not found for update: $id")
            return null
        }

        val updated = existing.copy(
            categoryId = updates.categoryId ?: existing.categoryId,
            title = updates.title,
            slug = updates.slug,
            shortDescription = updates.shortDescription ?: existing.shortDescription,
            description = updates.description ?: existing.description,
            status = updates.status,
            productType = updates.productType,
            brand = updates.brand ?: existing.brand,
            isCustomizable = updates.isCustomizable,
            isFeatured = updates.isFeatured,
            seoTitle = updates.seoTitle ?: existing.seoTitle,
            seoDescription = updates.seoDescription ?: existing.seoDescription,
            updatedAt = OffsetDateTime.now()
        )

        val saved = productRepo.update(id, updated) ?: run {
            logger.error("Failed to update product: $id")
            return null
        }

        // Update cache and invalidate queries
        productCache.handleStateMutation(id, saved)
        logger.info("Product updated: $id")
        return saved
    }

    suspend fun updateProductStatus(id: String, status: String): Product? {
        return productCache.updateStatus(id, status)
    }

    suspend fun softDeleteProduct(id: String): Product? {
        val deleted = productCache.softDelete(id)
        if (deleted != null) {
            logger.info("Product soft-deleted: $id")
        }
        return deleted
    }

    suspend fun restoreProduct(id: String): Product? {
        val restored = productCache.restore(id)
        if (restored != null) {
            logger.info("Product restored: $id")
        }
        return restored
    }

    suspend fun toggleProductFeatured(id: String): Product? {
        val toggled = productCache.toggleFeatured(id)
        if (toggled != null) {
            logger.info("Product featured toggled: $id - ${toggled.isFeatured}")
        }
        return toggled
    }

    suspend fun bulkUpdateProductStatus(ids: List<String>, status: String): Int {
        return productCache.bulkUpdateStatus(ids, status)
    }

    suspend fun deleteProduct(id: String): Boolean {
        val deleted = productRepo.delete(id)
        if (deleted) {
            productCache.removeFromCache(id)
            productCache.invalidateProductQueryCaches()
            logger.info("Product permanently deleted: $id")
        }
        return deleted
    }

    suspend fun bulkCreateProducts(products: List<Product>): List<Product> {
        val createdProducts = mutableListOf<Product>()

        for (product in products) {
            try {
                val created = createProduct(product)
                createdProducts.add(created)
            } catch (e: Exception) {
                logger.error("Failed to create product '${product.title}': ${e.message}", e)
            }
        }

        return createdProducts
    }

    suspend fun bulkDeleteProducts(ids: List<String>): Map<String, Boolean> {
        val results = mutableMapOf<String, Boolean>()

        for (id in ids) {
            results[id] = deleteProduct(id)
        }

        return results
    }


    data class ProductListResponse(
        val data: List<Map<String, Any?>>,
        val total: Long,
        val offset: Int,
        val limit: Int
    )


    suspend fun getProductsForAdmin(
        status: String? = null,
        productType: String? = null,
        brand: String? = null,
        search: String? = null,
        offset: Int = 0,
        limit: Int = 20
    ): ProductListResponse {
        // Get products based on available filters
        val products = when {
            // Search takes highest priority
            search != null -> productCache.searchProduct(search, offset, limit)

            // Combined filters - not directly cached, so we query repository and cache results
            status != null && productType != null -> {
                getProductsByStatusAndType(status, productType, offset, limit)
            }
            status != null && brand != null -> {
                getProductsByStatusAndBrand(status, brand, offset, limit)
            }

            // Single filters - use cached methods
            status != null -> productCache.findByStatus(status, offset, limit)
            productType != null -> productCache.findByProductType(productType, offset, limit)
            brand != null -> productCache.findByBrand(brand, offset, limit)

            // No filters - get all (limited set for admin listing)
            else -> getAllProducts(offset, limit)
        }

        // Count total for pagination
        val total = when {
            search != null -> countSearchResults(search)
            status != null && productType != null -> countByStatusAndType(status, productType)
            status != null && brand != null -> countByStatusAndBrand(status, brand)
            status != null -> countByStatus(status)
            productType != null -> countByType(productType)
            brand != null -> countByBrand(brand)
            else -> countAllProducts()
        }

        return ProductListResponse(
            data = Product.toRows(products),
            total = total,
            offset = offset,
            limit = limit
        )
    }

    private suspend fun getProductsByStatusAndType(
        status: String,
        productType: String,
        offset: Int,
        limit: Int
    ): List<Product> {
        val products = productCache.findByStatus(status, 0, Int.MAX_VALUE)
        return products
            .filter { it.productType == productType }
            .drop(offset)
            .take(limit)
    }

    private suspend fun getProductsByStatusAndBrand(
        status: String,
        brand: String,
        offset: Int,
        limit: Int
    ): List<Product> {
        val products = productCache.findByBrand(brand, 0, Int.MAX_VALUE)
        return products
            .filter { it.status == status }
            .drop(offset)
            .take(limit)
    }

    private suspend fun getAllProducts(offset: Int, limit: Int) : List<Product> {
        return productCache.findByStatus("published", offset, limit)
    }

    private suspend fun countSearchResults(query: String): Long {
        val results = productCache.searchProduct(query, 0, 1000)
        return results.size.toLong()
    }

    private suspend fun countByStatusAndType(status: String, productType: String): Long {
        val products = productCache.findByStatus(status, 0, Int.MAX_VALUE)
        return products.count { it.productType == productType }.toLong()
    }

    private suspend fun countByStatusAndBrand(status: String, brand: String): Long {
        val products = productCache.findByBrand(brand, 0, Int.MAX_VALUE)
        return products.count { it.status == status }.toLong()
    }

    private suspend fun countByStatus(status: String): Long {
        val products = productCache.findByStatus(status, 0, Int.MAX_VALUE)
        return products.size.toLong()
    }

    private suspend fun countByType(productType: String): Long {
        val products = productCache.findByProductType(productType, 0, Int.MAX_VALUE)
        return products.size.toLong()
    }

    private suspend fun countByBrand(brand: String): Long {
        val products = productCache.findByBrand(brand, 0, Int.MAX_VALUE)
        return products.size.toLong()
    }

    private suspend fun countAllProducts(): Long {
        // Get stats to get total count
        return productCache.getProductStats().totalProducts.toLong()
    }

    // --- Cache Management ---

    suspend fun refreshProductCache(id: String) {
        val product = productRepo.read(id)
        if (product != null) {
            productCache.putInCache(id, product)
        } else {
            productCache.removeFromCache(id)
        }
    }

    suspend fun warmupCache(limit: Int = 100) {
        logger.info("Starting cache warmup with limit: $limit")
        productRepo.readAll(0, limit).collect { product ->
            productCache.putInCache(product.id, product)
        }
        productCache.invalidateProductQueryCaches() // Force refresh of query caches
    }

    suspend fun invalidateAllCaches() {
        productCache.invalidateProductQueryCaches()
        logger.info("All product caches invalidated")
    }
}
