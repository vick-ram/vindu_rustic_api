package org.example.data.cache

import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.builtins.ListSerializer
import org.example.data.repo.CacheConfig
import org.example.data.repo.CrudCache
import org.example.data.repo.OrderRepository
import org.example.data.repo.OrderStats
import org.example.di.Component
import org.example.di.Inject
import org.example.domain.models.sales.Order
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.OffsetDateTime

@Component
@OptIn(ExperimentalLettuceCoroutinesApi::class)
class OrderCache @Inject constructor(
    private val redis: RedisCoroutinesCommands<String, String>,
    private val repository: OrderRepository,
    serializer: KSerializer<Order>
) : CrudCache<Order, String>(
    redis = redis,
    delegate = repository,
    getId = { it.id },
    serializer = serializer,
    config = object : CacheConfig {
        override val cacheName = "orders"
        override val ttl = 86400L // 24 hours baseline TTL
    }
) {
    private val logger: Logger = LoggerFactory.getLogger(OrderCache::class.java)
    private val orderListSerializer = ListSerializer(Order.serializer())

    /**
     * Intercept state updates to cleanly purge out-of-date records across all index views.
     */
    suspend fun updateStatus(
        id: String,
        status: String,
        paymentStatus: String? = null,
        fulfillmentStatus: String? = null
    ): Order? {
        val updatedOrder = repository.updateStatus(id, status, paymentStatus, fulfillmentStatus)
        if (updatedOrder != null) {
            // 1. Evict structural collections and secondary unique lookups
            redis.del("${config.cacheName}:number:${updatedOrder.orderNumber}")
            redis.del("${config.cacheName}:user:${updatedOrder.userId}")
            redis.del("${config.cacheName}:email:${updatedOrder.email}")

            // 2. Refresh the primary index cache immediately
            putInCache(updatedOrder.id, updatedOrder)

            // 3. Drop general dynamic collection groups (status buckets, search pools, stats)
            invalidateCollectionCaches()
        }
        return updatedOrder
    }

    /**
     * Secondary unique key index mapping order numbers to order IDs.
     */
    suspend fun findByOrderNumber(orderNumber: String): Order? {
        val lookupKey = "${config.cacheName}:number:$orderNumber"
        val cachedId = redis.get(lookupKey)

        if (cachedId != null) {
            // Route through base CrudCache to leverage the primary memory index
            return read(cachedId)
        }

        val freshOrder = repository.findByOrderNumber(orderNumber)
        if (freshOrder != null) {
            putInCache(freshOrder.id, freshOrder)
            try {
                redis.setex(lookupKey, config.ttl ?: 86400L, freshOrder.id)
            } catch (e: Exception) {
                logger.error("Failed to write order number index mapping", e)
            }
        }
        return freshOrder
    }

    /**
     * Cache customer specific order list views using a dedicated key space.
     */
    suspend fun findByUserId(userId: String, offset: Int = 0, limit: Int = 20): List<Order> {
        val cacheKey = "${config.cacheName}:user:$userId"
        return readCollection(cacheKey, offset, limit) {
            repository.findByUserId(userId, offset, limit)
        }
    }

    /**
     * Cache guest or support email lookups.
     */
    suspend fun findByEmail(email: String, offset: Int = 0, limit: Int = 20): List<Order> {
        val cacheKey = "${config.cacheName}:email:$email"
        return readCollection(cacheKey, offset, limit) {
            repository.findByEmail(email, offset, limit)
        }
    }

    /**
     * Paginated operational collections filtered by core workflow states.
     */
    suspend fun findByStatus(status: String, offset: Int = 0, limit: Int = 20): List<Order> {
        val params = mapOf("status" to status)
        return readAll(offset, limit, params)
    }

    suspend fun findByPaymentStatus(paymentStatus: String, offset: Int = 0, limit: Int = 20): List<Order> {
        val params = mapOf("paymentStatus" to paymentStatus)
        return readAll(offset, limit, params)
    }

    suspend fun findByFulfillmentStatus(fulfillmentStatus: String, offset: Int = 0, limit: Int = 20): List<Order> {
        val params = mapOf("fulfillmentStatus" to fulfillmentStatus)
        return readAll(offset, limit, params)
    }

    /**
     * Skip cache for full-text search pools to avoid excessive Redis fragmentation.
     */
    suspend fun searchOrders(query: String, status: String? = null, offset: Int = 0, limit: Int = 20): List<Order> {
        return repository.searchOrders(query, status, offset, limit)
    }

    /**
     * Dynamic date queries are executed directly against the database replica layer.
     */
    suspend fun findByDateRange(
        startDate: OffsetDateTime,
        endDate: OffsetDateTime,
        status: String? = null,
        offset: Int = 0,
        limit: Int = 50
    ): List<Order> {
        return repository.findByDateRange(startDate, endDate, status, offset, limit)
    }

    /**
     * Caches order statistics matrices with a brief, rolling time constraint.
     */
    suspend fun getOrderStats(startDate: OffsetDateTime? = null, endDate: OffsetDateTime? = null): OrderStats {
        val startToken = startDate?.toEpochSecond() ?: "all"
        val endToken = endDate?.toEpochSecond() ?: "all"
        val cacheKey = "${config.cacheName}:stats:$startToken:$endToken"

        val cachedJson = redis.get(cacheKey)
        if (cachedJson != null) {
            try {
                return Json.decodeFromString(OrderStats.serializer(), cachedJson)
            } catch (e: Exception) {
                logger.error("Failed to deserialize order statistics cache", e)
            }
        }

        val freshStats = repository.getOrderStats(startDate, endDate)
        try {
            // Keep stats short-lived (5 minutes) to avoid processing heavy table aggregates repeatedly
            redis.setex(cacheKey, 300L, Json.encodeToString(OrderStats.serializer(), freshStats))
        } catch (e: Exception) {
            logger.error("Failed to write order statistics cache", e)
        }
        return freshStats
    }

    /**
     * Sequence generations must bypass caches entirely to guarantee global data consistency.
     */
    suspend fun generateOrderNumber(): String {
        return repository.generateOrderNumber()
    }

    /**
     * Private helper to streamline user and email collection caching mechanics.
     */
    private suspend fun readCollection(
        cacheKey: String,
        offset: Int,
        limit: Int,
        fallback: suspend () -> List<Order>
    ): List<Order> {
        val partitionedKey = "$cacheKey:$offset:$limit"
        val cachedJson = redis.get(partitionedKey)

        if (cachedJson != null) {
            try {
                return Json.decodeFromString(orderListSerializer, cachedJson)
            } catch (e: Exception) {
                logger.error("Failed to deserialize order collection cache", e)
            }
        }

        val freshData = fallback()
        if (freshData.isNotEmpty()) {
            try {
                redis.setex(partitionedKey, config.ttl ?: 86400L, Json.encodeToString(orderListSerializer, freshData))
            } catch (e: Exception) {
                logger.error("Failed to write order collection cache", e)
            }
        }
        return freshData
    }
}