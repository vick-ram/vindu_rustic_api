package org.example.data.cache

import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.example.data.repo.CacheConfig
import org.example.data.repo.CrudCache
import org.example.data.repo.OrderStatusHistoryRepository
import org.example.data.repo.StatusDuration
import org.example.di.Component
import org.example.di.Inject
import org.example.domain.models.sales.OrderStatusHistory
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@Component
@OptIn(ExperimentalLettuceCoroutinesApi::class)
class OrderStatusHistoryCache @Inject constructor(
    private val redis: RedisCoroutinesCommands<String, String>,
    private val repository: OrderStatusHistoryRepository,
) : CrudCache<OrderStatusHistory, String>(
    redis = redis,
    delegate = repository,
    getId = { it.id },
    serializer = OrderStatusHistory.serializer(),
    config = object : CacheConfig {
        override val cacheName = "order_status_history"
        override val ttl = 86400L // 24 hours baseline TTL for append-only data
    }
) {
    private val logger: Logger = LoggerFactory.getLogger(OrderStatusHistoryCache::class.java)

    private val historyListSerializer = ListSerializer(OrderStatusHistory.serializer())
    private val durationListSerializer = ListSerializer(StatusDuration.serializer())

    /**
     * Intercept state logs to clear the localized query cache for the modified order.
     */
    suspend fun logStatusChange(
        orderId: String,
        oldStatus: String?,
        newStatus: String,
        changedBy: String? = null,
        comment: String? = null
    ): OrderStatusHistory {
        val historyEntry = repository.logStatusChange(orderId, oldStatus, newStatus, changedBy, comment)

        // Target explicit single-order query keys to prevent cache drift
        redis.del("${config.cacheName}:order:$orderId")
        redis.del("${config.cacheName}:latest:$orderId")
        redis.del("${config.cacheName}:duration:$orderId")

        // Invalidate broader staff/user overview collections
        invalidateCollectionCaches()

        // Seed the primary index cache with the fresh history row
        putInCache(historyEntry.id, historyEntry)

        return historyEntry
    }

    /**
     * Caches the entire historical timeline collection for an order.
     */
    suspend fun findByOrderId(orderId: String): List<OrderStatusHistory> {
        val cacheKey = "${config.cacheName}:order:$orderId"
        val cachedJson = redis.get(cacheKey)

        if (cachedJson != null) {
            try {
                return Json.decodeFromString(historyListSerializer, cachedJson)
            } catch (e: Exception) {
                logger.error("Failed to deserialize order status history timeline cache", e)
            }
        }

        val freshHistory = repository.findByOrderId(orderId)
        if (freshHistory.isNotEmpty()) {
            try {
                redis.setex(cacheKey, config.ttl ?: 86400L, Json.encodeToString(historyListSerializer, freshHistory))
            } catch (e: Exception) {
                logger.error("Failed to write order status history timeline cache", e)
            }
        }
        return freshHistory
    }

    /**
     * Cache the single most recent status change event record.
     */
    suspend fun getLatestStatusChange(orderId: String): OrderStatusHistory? {
        val cacheKey = "${config.cacheName}:latest:$orderId"
        val cachedJson = redis.get(cacheKey)

        if (cachedJson != null) {
            try {
                return Json.decodeFromString(OrderStatusHistory.serializer(), cachedJson)
            } catch (e: Exception) {
                logger.error("Failed to deserialize latest status change cache", e)
            }
        }

        val freshStatus = repository.getLatestStatusChange(orderId)
        if (freshStatus != null) {
            try {
                redis.setex(cacheKey, config.ttl ?: 86400L, Json.encodeToString(OrderStatusHistory.serializer(), freshStatus))
            } catch (e: Exception) {
                logger.error("Failed to write latest status change cache", e)
            }
        }
        return freshStatus
    }

    /**
     * Cache metrics detailing processing bottlenecks within individual fulfillment states.
     */
    suspend fun getStatusDuration(orderId: String): List<StatusDuration> {
        val cacheKey = "${config.cacheName}:duration:$orderId"
        val cachedJson = redis.get(cacheKey)

        if (cachedJson != null) {
            try {
                return Json.decodeFromString(durationListSerializer, cachedJson)
            } catch (e: Exception) {
                logger.error("Failed to deserialize status duration analysis cache", e)
            }
        }

        val freshDurations = repository.getStatusDuration(orderId)
        if (freshDurations.isNotEmpty()) {
            try {
                redis.setex(cacheKey, config.ttl ?: 86400L, Json.encodeToString(durationListSerializer, freshDurations))
            } catch (e: Exception) {
                logger.error("Failed to write status duration analysis cache", e)
            }
        }
        return freshDurations
    }

    /**
     * Leverage standard CrudCache multi-param mapping hooks for internal operations.
     */
    suspend fun findByChangedBy(changedBy: String, offset: Int = 0, limit: Int = 50): List<OrderStatusHistory> {
        val params = mapOf("changedBy" to changedBy)
        return readAll(offset, limit, params)
    }
}