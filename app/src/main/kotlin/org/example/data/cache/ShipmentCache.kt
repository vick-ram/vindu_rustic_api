package org.example.data.cache

import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.example.data.repo.CacheConfig
import org.example.data.repo.CrudCache
import org.example.data.repo.ShipmentRepository
import org.example.data.repo.ShipmentStats
import org.example.di.Component
import org.example.di.Inject
import org.example.domain.models.shipping.Shipment
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.time.OffsetDateTime

@OptIn(ExperimentalLettuceCoroutinesApi::class)
@Component
class ShipmentCache @Inject constructor(
    private val redis: RedisCoroutinesCommands<String, String>,
    private val shipmentRepo: ShipmentRepository
) : CrudCache<Shipment, String>(
    redis = redis,
    delegate = shipmentRepo,
    getId = { it.id },
    serializer = Shipment.serializer(),
    config = object : CacheConfig {
        override val cacheName = "shipments"
        override val ttl = 3600L
    }
) {
    private val logger: Logger = LoggerFactory.getLogger(ShipmentCache::class.java)
    private val shipmentListSerializer = ListSerializer(Shipment.serializer())

    /**
     * Shared read-through caching pipeline boilerplate reducer.
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

    // --- Overridden Crud Triggers ---

    override suspend fun create(model: Shipment): Shipment {
        val created = super.create(model)
        invalidateLookups(created)
        return created
    }

    override suspend fun update(id: String, entity: Shipment): Shipment? {
        val updated = super.update(id, entity)
        if (updated != null) {
            invalidateLookups(updated)
        }
        return updated
    }

    override suspend fun delete(id: String): Boolean {
        val existing = read(id)
        val deleted = super.delete(id)
        if (deleted && existing != null) {
            invalidateLookups(existing)
        }
        return deleted
    }

    // --- Domain Read Queries (Cached) ---

    suspend fun findByOrderId(orderId: String): List<Shipment> {
        val cacheKey = "${config.cacheName}:order:$orderId"
        return typedCacheOrFetch(cacheKey, shipmentListSerializer) {
            shipmentRepo.findByOrderId(orderId)
        }
    }

    suspend fun findByTrackingNumber(trackingNumber: String): Shipment? {
        val cacheKey = "${config.cacheName}:tracking:$trackingNumber"
        return typedCacheOrFetch(cacheKey, Shipment.serializer().nullable) {
            shipmentRepo.findByTrackingNumber(trackingNumber)
        }
    }

    suspend fun findByWarehouse(warehouseId: String, offset: Int = 0, limit: Int = 20): List<Shipment> {
        val cacheKey = "${config.cacheName}:warehouse:$warehouseId:$offset:$limit"
        return typedCacheOrFetch(cacheKey, shipmentListSerializer) {
            shipmentRepo.findByWarehouse(warehouseId, offset, limit)
        }
    }

    suspend fun findByCourier(courier: String, offset: Int = 0, limit: Int = 20): List<Shipment> {
        val cacheKey = "${config.cacheName}:courier:$courier:$offset:$limit"
        return typedCacheOrFetch(cacheKey, shipmentListSerializer) {
            shipmentRepo.findByCourier(courier, offset, limit)
        }
    }

    suspend fun findByDateRange(
        startDate: OffsetDateTime,
        endDate: OffsetDateTime,
        offset: Int = 0,
        limit: Int = 50
    ): List<Shipment> {
        val cacheKey = "${config.cacheName}:daterange:$startDate:$endDate:$offset:$limit"
        return typedCacheOrFetch(cacheKey, shipmentListSerializer) {
            shipmentRepo.findByDateRange(startDate, endDate, offset, limit)
        }
    }

    suspend fun getInTransitShipments(limit: Int = 50): List<Shipment> {
        val cacheKey = "${config.cacheName}:intransit:$limit"
        return typedCacheOrFetch(cacheKey, shipmentListSerializer) {
            shipmentRepo.getInTransitShipments(limit)
        }
    }

    suspend fun getOverdueShipments(): List<Shipment> {
        // Since this query acts against the current physical timestamp, it isn't completely static.
        // We use a dedicated transient key to avoid returning data that missed an SLA threshold.
        val cacheKey = "${config.cacheName}:overdue"
        return typedCacheOrFetch(cacheKey, shipmentListSerializer) {
            shipmentRepo.getOverdueShipments()
        }
    }

    suspend fun getShipmentStats(startDate: OffsetDateTime? = null, endDate: OffsetDateTime? = null): ShipmentStats {
        val cacheKey = "${config.cacheName}:stats:$startDate:$endDate"
        return typedCacheOrFetch(cacheKey, ShipmentStats.serializer()) {
            shipmentRepo.getShipmentStats(startDate, endDate)
        }
    }

    // --- Targeted Invalidation Mutations ---

    suspend fun updateTracking(id: String, trackingNumber: String, trackingUrl: String? = null, courier: String? = null): Shipment? {
        val original = read(id) // Pre-fetch to trace cross references if tracking parameters are changing
        val updated = shipmentRepo.updateTracking(id, trackingNumber, trackingUrl, courier)

        if (updated != null) {
            removeFromCache(id)
            invalidateLookups(updated)
            if (original != null && original.trackingNumber != trackingNumber) {
                removeFromCache("${config.cacheName}:tracking:${original.trackingNumber}")
            }
        }
        return updated
    }

    suspend fun markDelivered(id: String, deliveredAt: OffsetDateTime = OffsetDateTime.now()): Shipment? {
        val updated = shipmentRepo.markDelivered(id, deliveredAt)
        if (updated != null) {
            removeFromCache(id)
            invalidateLookups(updated)
            // Explicitly drops transient metrics pools
            removeFromCache("${config.cacheName}:overdue")
        }
        return updated
    }

    suspend fun updateShippingLabel(id: String, shippingLabelUrl: String): Shipment? {
        val updated = shipmentRepo.updateShippingLabel(id, shippingLabelUrl)
        if (updated != null) {
            removeFromCache(id)
            invalidateLookups(updated)
        }
        return updated
    }

    // --- Complex Multi-Index Eviction Helpers ---

    /**
     * Drops analytical matrices and contextual lookups when any base mutation touches a shipment record.
     */
    private suspend fun invalidateLookups(shipment: Shipment) {
        invalidateCollectionCaches()
        removeFromCache("${config.cacheName}:order:${shipment.orderId}")
        removeFromCache("${config.cacheName}:tracking:${shipment.trackingNumber}")

        // Wipe cross-cutting pattern dimensions
        deleteKeysByPattern("${config.cacheName}:warehouse:${shipment.warehouseId}:*")
        deleteKeysByPattern("${config.cacheName}:courier:${shipment.courier}:*")
        deleteKeysByPattern("${config.cacheName}:daterange:*")
        deleteKeysByPattern("${config.cacheName}:intransit:*")
        deleteKeysByPattern("${config.cacheName}:stats:*")
    }
}

/**
 * Extension property facilitating safe compilation with nullable objects.
 */
private val <T> kotlinx.serialization.KSerializer<T>.nullable: kotlinx.serialization.KSerializer<T?>
    get() = @Suppress("UNCHECKED_CAST") (this as kotlinx.serialization.KSerializer<T?>)
