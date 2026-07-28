package org.example.data.cache

import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.example.data.repo.CacheConfig
import org.example.data.repo.CrudCache
import org.example.data.repo.ProductRepository
import org.example.data.repo.ProductSearchResult
import org.example.data.repo.ProductStats
import org.example.data.repo.ProductWithStock
import org.example.domain.models.catalog.Product
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.OffsetDateTime

@OptIn(ExperimentalLettuceCoroutinesApi::class)
class ProductCache(
    private val redis: RedisCoroutinesCommands<String, String>,
    private val productRepo: ProductRepository,
    config: CacheConfig
) : CrudCache<Product, String>(
    redis = redis,
    delegate = productRepo,
    getId = { it.id }, // Assuming Product has an 'id' property of type String
    serializer = Product.serializer(),
    config = config
) {
    private val logger: Logger = LoggerFactory.getLogger(ProductCache::class.java)
    private val productListSerializer = ListSerializer(Product.serializer())

    /**
     * Helper to cache collections with custom sub-keys (e.g., category, status, featured)
     */
    private suspend fun <T> typedCacheOrFetch(
        cacheKey: String,
        serializer: kotlinx.serialization.KSerializer<T>,
        fetcher: suspend () -> T
    ): T {
        val cachedJson = try {
            redis.get(cacheKey)
        } catch (e: Exception) {
            logger.error("Failed to read from cache for key: $cacheKey", e)
            null
        }

        if (cachedJson != null) {
            try {
                return Json.decodeFromString(serializer, cachedJson)
            } catch (e: Exception) {
                logger.error("Failed to deserialize cache for key: $cacheKey", e)
            }
        }

        val result = fetcher()

        // Cache the result if it's a non-empty list or a non-null object
        val shouldCache = when (result) {
            is List<*> -> result.isNotEmpty()
            null -> false
            else -> true
        }

        if (shouldCache) {
            try {
                val jsonValue = Json.encodeToString(serializer, result)
                if (config.ttl != null) {
                    redis.setex(cacheKey, config.ttl!!, jsonValue)
                } else {
                    redis.set(cacheKey, jsonValue)
                }
            } catch (e: Exception) {
                logger.error("Failed to write to cache for key: $cacheKey", e)
            }
        }

        return result
    }

    // --- Query Methods (Cached) ---

    suspend fun findBySlug(slug: String, includeDeleted: Boolean = false): Product? {
        val cacheKey = "${config.cacheName}:slug:$slug:$includeDeleted"
        return typedCacheOrFetch(cacheKey, Product.serializer().nullable) {
            productRepo.findBySlug(slug, includeDeleted)
        }
    }

    suspend fun findByCategoryId(
        categoryId: String,
        status: String? = "published",
        offset: Int = 0,
        limit: Int = 20
    ): List<Product> {
        val cacheKey = "${config.cacheName}:category:$categoryId:$status:$offset:$limit"
        return typedCacheOrFetch(cacheKey, productListSerializer) {
            productRepo.findByCategoryId(categoryId, status, offset, limit)
        }
    }

    suspend fun findByStatus(status: String, offset: Int = 0, limit: Int = 20): List<Product> {
        val cacheKey = "${config.cacheName}:status:$status:$offset:$limit"
        return typedCacheOrFetch(cacheKey, productListSerializer) {
            productRepo.findByStatus(status, offset, limit)
        }
    }

    suspend fun findFeatured(offset: Int = 0, limit: Int = 20): List<Product> {
        val cacheKey = "${config.cacheName}:featured:$offset:$limit"
        return typedCacheOrFetch(cacheKey, productListSerializer) {
            productRepo.findFeatured(offset, limit)
        }
    }

    suspend fun findCustomizable(offset: Int = 0, limit: Int = 20): List<Product> {
        val cacheKey = "${config.cacheName}:customizable:$offset:$limit"
        return typedCacheOrFetch(cacheKey, productListSerializer) {
            productRepo.findCustomizable(offset, limit)
        }
    }

    suspend fun findByBrand(brand: String, offset: Int = 0, limit: Int = 20): List<Product> {
        val cacheKey = "${config.cacheName}:brand:$brand:$offset:$limit"
        return typedCacheOrFetch(cacheKey, productListSerializer) {
            productRepo.findByBrand(brand, offset, limit)
        }
    }

    suspend fun findByProductType(productType: String, offset: Int = 0, limit: Int = 20): List<Product> {
        val cacheKey = "${config.cacheName}:type:$productType:$offset:$limit"
        return typedCacheOrFetch(cacheKey, productListSerializer) {
            productRepo.findByProductType(productType, offset, limit)
        }
    }

    suspend fun searchProduct(query: String, offset: Int = 0, limit: Int = 20): List<Product> {
        // Search queries are highly dynamic; cached under a search namespace
        val cacheKey = "${config.cacheName}:search:$query:$offset:$limit"
        return typedCacheOrFetch(cacheKey, productListSerializer) {
            productRepo.searchProduct(query, offset, limit)
        }
    }

    suspend fun fullTextSearch(query: String, offset: Int = 0, limit: Int = 20): List<ProductSearchResult> {
        val cacheKey = "${config.cacheName}:fts:$query:$offset:$limit"
        return typedCacheOrFetch(cacheKey, ListSerializer(ProductSearchResult.serializer())) {
            productRepo.fullTextSearch(query, offset, limit)
        }
    }

    suspend fun findRelated(productId: String, limit: Int = 10): List<Product> {
        val cacheKey = "${config.cacheName}:related:$productId:$limit"
        return typedCacheOrFetch(cacheKey, productListSerializer) {
            productRepo.findRelated(productId, limit)
        }
    }

    suspend fun getProductStats(): ProductStats {
        val cacheKey = "${config.cacheName}:stats"
        return typedCacheOrFetch(cacheKey, ProductStats.serializer()) {
            productRepo.getProductStats()
        }
    }

    suspend fun findByDateRange(
        startDate: OffsetDateTime,
        endDate: OffsetDateTime,
        status: String? = null,
        offset: Int = 0,
        limit: Int = 50
    ): List<Product> {
        val cacheKey = "${config.cacheName}:daterange:${startDate.toEpochSecond()}:${endDate.toEpochSecond()}:$status:$offset:$limit"
        return typedCacheOrFetch(cacheKey, productListSerializer) {
            productRepo.findByDateRange(startDate, endDate, status, offset, limit)
        }
    }

    suspend fun getLatest(limit: Int = 10, status: String = "published"): List<Product> {
        val cacheKey = "${config.cacheName}:latest:$status:$limit"
        return typedCacheOrFetch(cacheKey, productListSerializer) {
            productRepo.getLatest(limit, status)
        }
    }

    suspend fun isSlugUnique(slug: String, excludeId: String? = null): Boolean {
        // We bypass cache for absolute constraint validations to ensure zero race-conditions
        return productRepo.isSlugUnique(slug, excludeId)
    }

    suspend fun getLowStockProducts(warehouseId: String? = null, threshold: Int = 10, limit: Int = 20): List<ProductWithStock> {
        // Real-time stock shouldn't be long-cached, but we cache with short parameters if needed
        val cacheKey = "${config.cacheName}:lowstock:$warehouseId:$threshold:$limit"
        return typedCacheOrFetch(cacheKey, ListSerializer(ProductWithStock.serializer())) {
            productRepo.getLowStockProducts(warehouseId, threshold, limit)
        }
    }

    // --- Write Actions (Evict & Sync Cache) ---

    suspend fun updateStatus(id: String, status: String): Product? {
        val updated = productRepo.updateStatus(id, status)
        handleStateMutation(id, updated)
        return updated
    }

    suspend fun softDelete(id: String): Product? {
        val updated = productRepo.softDelete(id)
        handleStateMutation(id, updated)
        return updated
    }

    suspend fun restore(id: String): Product? {
        val updated = productRepo.restore(id)
        handleStateMutation(id, updated)
        return updated
    }

    suspend fun toggleFeatured(id: String): Product? {
        val updated = productRepo.toggleFeatured(id)
        handleStateMutation(id, updated)
        return updated
    }

    suspend fun bulkUpdateStatus(ids: List<String>, status: String): Int {
        val rowsUpdated = productRepo.bulkUpdateStatus(ids, status)
        if (rowsUpdated > 0) {
            // Drop individual item keys and wipe all structured query/collection indices
            ids.forEach { removeFromCache(it) }
            invalidateProductQueryCaches()
        }
        return rowsUpdated
    }

    // --- Private Cache Eviction Orchestration ---

    private suspend fun handleStateMutation(id: String, updatedProduct: Product?) {
        if (updatedProduct != null) {
            putInCache(id, updatedProduct)
        } else {
            removeFromCache(id)
        }
        invalidateProductQueryCaches()
    }

    /**
     * Drops all query collections and indices from Redis while keeping base entity caches active.
     */
    private suspend fun invalidateProductQueryCaches() {
        invalidateCollectionCaches() // Base method handling `cacheName:collection:*`
        deleteKeysByPattern("${config.cacheName}:slug:*")
        deleteKeysByPattern("${config.cacheName}:category:*")
        deleteKeysByPattern("${config.cacheName}:status:*")
        deleteKeysByPattern("${config.cacheName}:featured:*")
        deleteKeysByPattern("${config.cacheName}:customizable:*")
        deleteKeysByPattern("${config.cacheName}:brand:*")
        deleteKeysByPattern("${config.cacheName}:type:*")
        deleteKeysByPattern("${config.cacheName}:search:*")
        deleteKeysByPattern("${config.cacheName}:fts:*")
        deleteKeysByPattern("${config.cacheName}:related:*")
        deleteKeysByPattern("${config.cacheName}:stats*")
        deleteKeysByPattern("${config.cacheName}:daterange:*")
        deleteKeysByPattern("${config.cacheName}:latest:*")
        deleteKeysByPattern("${config.cacheName}:lowstock:*")
    }
}

/**
 * Kotlinx Serialization Extension to cleanly serialize nullable products inside a lambda block.
 */
private val <T> kotlinx.serialization.KSerializer<T>.nullable: kotlinx.serialization.KSerializer<T?>
    get() = @Suppress("UNCHECKED_CAST") (this as kotlinx.serialization.KSerializer<T?>)