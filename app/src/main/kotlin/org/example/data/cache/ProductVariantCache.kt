package org.example.data.cache

import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.example.data.repo.CacheConfig
import org.example.data.repo.CrudCache
import org.example.data.repo.ProductVariantRepository
import org.example.data.repo.PriceStats
import org.example.domain.models.catalog.ProductVariant
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.math.BigDecimal

@OptIn(ExperimentalLettuceCoroutinesApi::class)
class ProductVariantCache(
    private val redis: RedisCoroutinesCommands<String, String>,
    private val variantRepo: ProductVariantRepository,
    config: CacheConfig
) : CrudCache<ProductVariant, String>(
    redis = redis,
    delegate = variantRepo,
    getId = { it.id }, // Assumes ProductVariant has an 'id' field of type String
    serializer = ProductVariant.serializer(),
    config = config
) {
    private val logger: Logger = LoggerFactory.getLogger(ProductVariantCache::class.java)
    private val variantListSerializer = ListSerializer(ProductVariant.serializer())

    /**
     * Shared helper to orchestrate local read-through caching behaviors seamlessly.
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

    // --- Overridden Crud Operations to handle indexing hooks ---

    override suspend fun create(model: ProductVariant): ProductVariant {
        val created = super.create(model)
        invalidateVariantQueryCaches(created.productId)
        return created
    }

    override suspend fun update(id: String, entity: ProductVariant): ProductVariant? {
        val updated = super.update(id, entity)
        if (updated != null) {
            invalidateVariantQueryCaches(updated.productId)
        }
        return updated
    }

    override suspend fun delete(id: String): Boolean {
        // We fetch the item right before deletion so we know its parent productId to invalidate correctly
        val existing = read(id)
        val deleted = super.delete(id)
        if (deleted && existing != null) {
            invalidateVariantQueryCaches(existing.productId)
        }
        return deleted
    }

    // --- Domain Read Queries (Cached) ---

    suspend fun findByProductId(productId: String): List<ProductVariant> {
        val cacheKey = "${config.cacheName}:product:$productId"
        return typedCacheOrFetch(cacheKey, variantListSerializer) {
            variantRepo.findByProductId(productId)
        }
    }

    suspend fun findBySku(sku: String): ProductVariant? {
        val cacheKey = "${config.cacheName}:sku:$sku"
        return typedCacheOrFetch(cacheKey, ProductVariant.serializer().nullable) {
            variantRepo.findBySku(sku)
        }
    }

    suspend fun findByPriceRange(
        minPrice: BigDecimal,
        maxPrice: BigDecimal,
        offset: Int = 0,
        limit: Int = 20
    ): List<ProductVariant> {
        val cacheKey = "${config.cacheName}:pricerange:$minPrice:$maxPrice:$offset:$limit"
        return typedCacheOrFetch(cacheKey, variantListSerializer) {
            variantRepo.findByPriceRange(minPrice, maxPrice, offset, limit)
        }
    }

    suspend fun getPriceStats(productId: String): PriceStats? {
        val cacheKey = "${config.cacheName}:stats:$productId"
        return typedCacheOrFetch(cacheKey, PriceStats.serializer().nullable) {
            variantRepo.getPriceStats(productId)
        }
    }

    suspend fun deactivate(id: String): Boolean {
        val deactivated = variantRepo.deactivate(id)
        if (deactivated) {
            val existing = read(id)
            removeFromCache(id) // Erase single object cache
            if (existing != null) {
                invalidateVariantQueryCaches(existing.productId)
            }
        }
        return deactivated
    }

    suspend fun bulkUpdatePrices(productId: String, priceMultiplier: BigDecimal): Int {
        val rowsUpdated = variantRepo.bulkUpdatePrices(productId, priceMultiplier)
        if (rowsUpdated > 0) {
            // Because several variant values changed on the database, we drop individual entity records matching this product
            evictIndividualVariantsByProduct(productId)
            // Wipe out collective queries (stats, product groupings, ranges)
            invalidateVariantQueryCaches(productId)
        }
        return rowsUpdated
    }

    /**
     * Wipes query spaces affected by alterations under a specific Product boundary.
     */
    private suspend fun invalidateVariantQueryCaches(productId: String) {
        invalidateCollectionCaches() // Inherited drops `cacheName:collection:*`
        removeFromCache("${config.cacheName}:product:$productId")
        removeFromCache("${config.cacheName}:stats:$productId")

        // SKU and Price Range indexes are complex cross-sections, so their search trees drop out
        deleteKeysByPattern("${config.cacheName}:sku:*")
        deleteKeysByPattern("${config.cacheName}:pricerange:*")
    }

    /**
     * Drops single entity caches linked directly back to a parent product.
     * Uses the cached collection index to discover the exact IDs without triggering an extensive database scan.
     */
    private suspend fun evictIndividualVariantsByProduct(productId: String) {
        val cachedProductVariants = findByProductId(productId)
        if (cachedProductVariants.isNotEmpty()) {
            cachedProductVariants.forEach { variant ->
                removeFromCache(variant.id)
            }
        }
    }
}

/**
 * Extension properties ensuring syntax compilation clarity with nullable targets.
 */
private val <T> kotlinx.serialization.KSerializer<T>.nullable: kotlinx.serialization.KSerializer<T?>
    get() = @Suppress("UNCHECKED_CAST") (this as kotlinx.serialization.KSerializer<T?>)