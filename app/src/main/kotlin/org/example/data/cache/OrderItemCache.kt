package org.example.data.cache

import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.example.data.repo.*
import org.example.di.Component
import org.example.di.Inject
import org.example.domain.models.sales.OrderItem
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.OffsetDateTime

@OptIn(ExperimentalLettuceCoroutinesApi::class)
@Component
class OrderItemCache @Inject constructor(
    private val redis: RedisCoroutinesCommands<String, String>,
    private val repository: OrderItemRepository,
) : CrudCache<OrderItem, String>(
    redis = redis,
    delegate = repository,
    getId = { it.id },
    serializer = OrderItem.serializer(),
    config = object : CacheConfig {
        override val cacheName = "order_items"
        override val ttl = 86400L // 24 hours default TTL — historical data is highly immutable
    }
) {
    private val logger: Logger = LoggerFactory.getLogger(OrderItemCache::class.java)

    private val orderItemListSerializer = ListSerializer(OrderItem.serializer())
    private val orderItemWithProductListSerializer = ListSerializer(OrderItemWithProduct.serializer())
    private val topSellingProductListSerializer = ListSerializer(TopSellingProduct.serializer())

    /**
     * Intercept bulk creations to accurately place items inside Redis and trigger invalidation hooks.
     */
    suspend fun bulkCreate(items: List<OrderItem>): List<OrderItem> {
        val createdItems = repository.bulkCreate(items)

        if (createdItems.isNotEmpty()) {
            // Contextually isolate parent groupings to minimize invalidation scopes
            val orderId = createdItems.first().orderId
            redis.del("${config.cacheName}:order:$orderId")
            redis.del("${config.cacheName}:order-details:$orderId")

            // Proactively cache individual rows to seed the primary data index
            createdItems.forEach { item ->
                putInCache(item.id, item)
            }

            // Clear standard structural query collections
            invalidateCollectionCaches()
        }
        return createdItems
    }

    /**
     * Cache items tied to a single order. Highly structured.
     */
    suspend fun findByOrderId(orderId: String): List<OrderItem> {
        val cacheKey = "${config.cacheName}:order:$orderId"
        val cachedJson = redis.get(cacheKey)

        if (cachedJson != null) {
            try {
                return Json.decodeFromString(orderItemListSerializer, cachedJson)
            } catch (e: Exception) {
                logger.error("Failed to deserialize order items collection cache", e)
            }
        }

        val freshItems = repository.findByOrderId(orderId)
        if (freshItems.isNotEmpty()) {
            try {
                redis.setex(cacheKey, config.ttl ?: 86400L, Json.encodeToString(orderItemListSerializer, freshItems))
            } catch (e: Exception) {
                logger.error("Failed to write order items collection cache", e)
            }
        }
        return freshItems
    }

    /**
     * Cache complex table-join operations containing external metadata values.
     */
    suspend fun findByOrderIdWithProductDetails(orderId: String): List<OrderItemWithProduct> {
        val cacheKey = "${config.cacheName}:order-details:$orderId"
        val cachedJson = redis.get(cacheKey)

        if (cachedJson != null) {
            try {
                return Json.decodeFromString(orderItemWithProductListSerializer, cachedJson)
            } catch (e: Exception) {
                logger.error("Failed to deserialize order product details cache", e)
            }
        }

        val freshDetails = repository.findByOrderIdWithProductDetails(orderId)
        if (freshDetails.isNotEmpty()) {
            try {
                redis.setex(cacheKey, config.ttl ?: 86400L, Json.encodeToString(orderItemWithProductListSerializer, freshDetails))
            } catch (e: Exception) {
                logger.error("Failed to write order product details cache", e)
            }
        }
        return freshDetails
    }

    /**
     * Cache lookups scoped by master Product Identity utilizing CrudCache standard hooks.
     */
    suspend fun findByProductId(productId: String, offset: Int = 0, limit: Int = 20): List<OrderItem> {
        val params = mapOf("productId" to productId)
        return readAll(offset, limit, params)
    }

    /**
     * Cache lookups scoped by isolated Variant Identity utilizing CrudCache standard hooks.
     */
    suspend fun findByVariantId(variantId: String, offset: Int = 0, limit: Int = 20): List<OrderItem> {
        val params = mapOf("variantId" to variantId)
        return readAll(offset, limit, params)
    }

    /**
     * Caches computational heavy aggregation analytics.
     */
    suspend fun getTopSellingProducts(
        limit: Int = 10,
        startDate: OffsetDateTime? = null,
        endDate: OffsetDateTime? = null
    ): List<TopSellingProduct> {
        // Construct a structural key based on optional date boundaries
        val startToken = startDate?.toEpochSecond() ?: "all"
        val endToken = endDate?.toEpochSecond() ?: "all"
        val cacheKey = "${config.cacheName}:top-selling:$limit:$startToken:$endToken"

        val cachedJson = redis.get(cacheKey)
        if (cachedJson != null) {
            try {
                return Json.decodeFromString(topSellingProductListSerializer, cachedJson)
            } catch (e: Exception) {
                logger.error("Failed to deserialize top selling products analytic cache", e)
            }
        }

        val freshAnalytics = repository.getTopSellingProducts(limit, startDate, endDate)
        try {
            // Keep analysis shorter-lived (e.g. 1 hour) to ensure metrics update periodically
            redis.setex(cacheKey, 3600L, Json.encodeToString(topSellingProductListSerializer, freshAnalytics))
        } catch (e: Exception) {
            logger.error("Failed to write top selling products analytic cache", e)
        }
        return freshAnalytics
    }
}