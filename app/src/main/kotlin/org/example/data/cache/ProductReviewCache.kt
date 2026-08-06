package org.example.data.cache

import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import org.example.data.repo.CacheConfig
import org.example.data.repo.CrudCache
import org.example.data.repo.ProductReviewRepository
import org.example.di.Component
import org.example.domain.models.catalog.ProductReview
import org.slf4j.LoggerFactory

@OptIn(ExperimentalLettuceCoroutinesApi::class)
@Component
class ProductReviewCache(
    private val redis: RedisCoroutinesCommands<String, String>,
    private val delegate: ProductReviewRepository
) : CrudCache<ProductReview, String>(
    redis = redis,
    delegate = delegate,
    getId = { it.id },
    serializer = ProductReview.serializer(),
    config = object : CacheConfig {
        override val cacheName = "product_reviews"
        override val ttl = 3600L // 1 hour TTL
    }
) {

    private val logger = LoggerFactory.getLogger(ProductReviewCache::class.java)
    private val collectionSerializer = ListSerializer(ProductReview.serializer())

    // Cache key generators for custom queries
    private fun generateFindByProductIdKey(productId: String, offset: Int, limit: Int): String {
        return "${config.cacheName}:findByProductId:$productId:$offset:$limit"
    }

    private fun generateFindByUserIdKey(userId: String): String {
        return "${config.cacheName}:findByUserId:$userId"
    }

    private fun generateFindVerifiedByProductIdKey(productId: String): String {
        return "${config.cacheName}:findVerifiedByProductId:$productId"
    }

    private fun generateGetAverageRatingKey(productId: String): String {
        return "${config.cacheName}:avgRating:$productId"
    }

    private fun generateGetRatingDistributionKey(productId: String): String {
        return "${config.cacheName}:ratingDistribution:$productId"
    }

    // Cached custom query methods
    suspend fun findByProductId(
        productId: String,
        offset: Int = 0,
        limit: Int = 20
    ): List<ProductReview> {
        val cacheKey = generateFindByProductIdKey(productId, offset, limit)

        // Try cache first
        val cachedResult = getListFromCache(cacheKey)
        if (cachedResult != null) return cachedResult

        // Fetch from delegate and cache
        val reviews = delegate.findByProductId(productId, offset, limit)
        cacheList(cacheKey, reviews)
        return reviews
    }

    suspend fun findByUserId(userId: String): List<ProductReview> {
        val cacheKey = generateFindByUserIdKey(userId)

        // Try cache first
        val cachedResult = getListFromCache(cacheKey)
        if (cachedResult != null) return cachedResult

        // Fetch from delegate and cache
        val reviews = delegate.findByUserId(userId)
        cacheList(cacheKey, reviews)
        return reviews
    }

    suspend fun findVerifiedByProductId(productId: String): List<ProductReview> {
        val cacheKey = generateFindVerifiedByProductIdKey(productId)

        // Try cache first
        val cachedResult = getListFromCache(cacheKey)
        if (cachedResult != null) return cachedResult

        // Fetch from delegate and cache
        val reviews = delegate.findVerifiedByProductId(productId)
        cacheList(cacheKey, reviews)
        return reviews
    }

    suspend fun getAverageRating(productId: String): Double? {
        val cacheKey = generateGetAverageRatingKey(productId)

        // Try cache first
        val cachedResult = getDoubleFromCache(cacheKey)
        if (cachedResult != null) return cachedResult

        // Fetch from delegate and cache
        val avgRating = delegate.getAverageRating(productId)
        if (avgRating != null) {
            cacheDouble(cacheKey, avgRating)
        }
        return avgRating
    }

    suspend fun getRatingDistribution(productId: String): Map<Int, Int> {
        val cacheKey = generateGetRatingDistributionKey(productId)

        // Try cache first
        val cachedResult = getRatingDistributionFromCache(cacheKey)
        if (cachedResult != null) return cachedResult

        // Fetch from delegate and cache
        val distribution = delegate.getRatingDistribution(productId)
        cacheRatingDistribution(cacheKey, distribution)
        return distribution
    }

    // Override invalidate methods to also clear custom query caches
    override suspend fun create(model: ProductReview): ProductReview {
        val created = super.create(model)
        invalidateProductCaches(model.productId)
        return created
    }

    override suspend fun update(id: String, entity: ProductReview): ProductReview? {
        val updated = super.update(id, entity)
        if (updated != null) {
            invalidateProductCaches(updated.productId)
        }
        return updated
    }

    override suspend fun delete(id: String): Boolean {
        // Get the review first to know the product ID for cache invalidation
        val review = read(id)
        val deleted = super.delete(id)
        if (deleted && review != null) {
            invalidateProductCaches(review.productId)
        }
        return deleted
    }

    // Helper methods for cache operations
    @OptIn(ExperimentalLettuceCoroutinesApi::class)
    private suspend fun getListFromCache(key: String): List<ProductReview>? {
        return try {
            redis.get(key)?.let { json ->
                Json.decodeFromString(collectionSerializer, json)
            }
        } catch (e: Exception) {
            logger.error("Failed to get list from cache for key: $key", e)
            null
        }
    }

    @OptIn(ExperimentalLettuceCoroutinesApi::class)
    private suspend fun cacheList(key: String, list: List<ProductReview>) {
        if (list.isNotEmpty()) {
            try {
                val jsonValue = Json.encodeToString(collectionSerializer, list)
                if (config.ttl != null) {
                    redis.setex(key, config.ttl!!, jsonValue)
                } else {
                    redis.set(key, jsonValue)
                }
            } catch (e: Exception) {
                logger.error("Failed to cache list for key: $key", e)
            }
        }
    }

    @OptIn(ExperimentalLettuceCoroutinesApi::class)
    private suspend fun getDoubleFromCache(key: String): Double? {
        return try {
            redis.get(key)?.toDoubleOrNull()
        } catch (e: Exception) {
            logger.error("Failed to get double from cache for key: $key", e)
            null
        }
    }

    @OptIn(ExperimentalLettuceCoroutinesApi::class)
    private suspend fun cacheDouble(key: String, value: Double) {
        try {
            if (config.ttl != null) {
                redis.setex(key, config.ttl!!, value.toString())
            } else {
                redis.set(key, value.toString())
            }
        } catch (e: Exception) {
            logger.error("Failed to cache double for key: $key", e)
        }
    }

    @OptIn(ExperimentalLettuceCoroutinesApi::class)
    private suspend fun getRatingDistributionFromCache(key: String): Map<Int, Int>? {
        return try {
            redis.get(key)?.let { json ->
                Json.decodeFromString(
                    MapSerializer(Int.serializer(), Int.serializer()),
                    json
                )
            }
        } catch (e: Exception) {
            logger.error("Failed to get rating distribution from cache for key: $key", e)
            null
        }
    }

    @OptIn(ExperimentalLettuceCoroutinesApi::class)
    private suspend fun cacheRatingDistribution(key: String, distribution: Map<Int, Int>) {
        try {
            val jsonValue = Json.encodeToString(
                MapSerializer(Int.serializer(), Int.serializer()),
                distribution
            )
            if (config.ttl != null) {
                redis.setex(key, config.ttl!!, jsonValue)
            } else {
                redis.set(key, jsonValue)
            }
        } catch (e: Exception) {
            logger.error("Failed to cache rating distribution for key: $key", e)
        }
    }

    // Invalidate all caches related to a product
    suspend fun invalidateProductCaches(productId: String) {
        logger.debug("Invalidating all caches for product: $productId")
        invalidateCollectionCaches()
        deleteKeysByPattern("${config.cacheName}:findByProductId:$productId:*")
        deleteKeysByPattern("${config.cacheName}:findVerifiedByProductId:$productId")
        deleteKeysByPattern("${config.cacheName}:avgRating:$productId")
        deleteKeysByPattern("${config.cacheName}:ratingDistribution:$productId")
    }

    // Invalidate user-specific caches
    suspend fun invalidateUserCaches(userId: String) {
        logger.debug("Invalidating all caches for user: $userId")
        deleteKeysByPattern("${config.cacheName}:findByUserId:$userId")
    }

    // Clear all product review caches
    suspend fun clearAllCaches() {
        logger.info("Clearing all product review caches")
        clearCache()
    }
}