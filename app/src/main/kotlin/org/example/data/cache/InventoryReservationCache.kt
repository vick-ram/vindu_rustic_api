package org.example.data.cache

import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.example.data.repo.CacheConfig
import org.example.data.repo.CrudCache
import org.example.data.repo.InventoryReservationRepository
import org.example.di.Component
import org.example.di.Inject
import org.example.domain.models.inventory.InventoryReservation
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@Component
@OptIn(ExperimentalLettuceCoroutinesApi::class)
class InventoryReservationCache @Inject constructor(
    private val redis: RedisCoroutinesCommands<String, String>,
    private val repository: InventoryReservationRepository,
) : CrudCache<InventoryReservation, String>(
    redis = redis,
    delegate = repository,
    getId = { it.id },
    serializer = InventoryReservation.serializer(),
    config = object : CacheConfig {
        override val cacheName = "inventory_reservations"
        override val ttl = 900L // Short 15-minute TTL matching default reservation lengths
    }
) {
    private val logger: Logger = LoggerFactory.getLogger(InventoryReservationCache::class.java)
    private val reservationListSerializer = ListSerializer(InventoryReservation.serializer())

    /**
     * Cache Wrapper for lookups by Variant.
     * CAUTION: Time sensitive. We assign a very short TTL to prevent stale visible arrays.
     */
    suspend fun findByVariantId(variantId: String): List<InventoryReservation> {
        val cacheKey = "${config.cacheName}:variant:$variantId"
        val cachedJson = redis.get(cacheKey)

        if (cachedJson != null) {
            try {
                return Json.decodeFromString(reservationListSerializer, cachedJson)
            } catch (e: Exception) {
                logger.error("Failed to deserialize variant reservations cache", e)
            }
        }

        val freshReservations = repository.findByVariantId(variantId)
        try {
            // Keep it down to a 30-second cache window because time is marching forward
            redis.setex(cacheKey, 30L, Json.encodeToString(reservationListSerializer, freshReservations))
        } catch (e: Exception) {
            logger.error("Failed to write variant reservations cache", e)
        }
        return freshReservations
    }

    /**
     * Cache Wrapper for lookups by Cart.
     */
    suspend fun findByCartId(cartId: String): List<InventoryReservation> {
        val cacheKey = "${config.cacheName}:cart:$cartId"
        val cachedJson = redis.get(cacheKey)

        if (cachedJson != null) {
            try {
                return Json.decodeFromString(reservationListSerializer, cachedJson)
            } catch (e: Exception) {
                logger.error("Failed to deserialize cart reservations cache", e)
            }
        }

        val freshReservations = repository.findByCartId(cartId)
        try {
            redis.setex(cacheKey, config.ttl ?: 900L, Json.encodeToString(reservationListSerializer, freshReservations))
        } catch (e: Exception) {
            logger.error("Failed to write cart reservations cache", e)
        }
        return freshReservations
    }

    /**
     * Cache Wrapper for lookups by Order.
     */
    suspend fun findByOrderId(orderId: String): List<InventoryReservation> {
        val cacheKey = "${config.cacheName}:order:$orderId"
        val cachedJson = redis.get(cacheKey)

        if (cachedJson != null) {
            try {
                return Json.decodeFromString(reservationListSerializer, cachedJson)
            } catch (e: Exception) {
                logger.error("Failed to deserialize order reservations cache", e)
            }
        }

        val freshReservations = repository.findByOrderId(orderId)
        try {
            redis.setex(cacheKey, config.ttl ?: 900L, Json.encodeToString(reservationListSerializer, freshReservations))
        } catch (e: Exception) {
            logger.error("Failed to write order reservations cache", e)
        }
        return freshReservations
    }

    /**
     * CRITICAL QUANTITY METRICS: We bypass caching for calculating quantities to avoid over-allocation race conditions.
     */
    suspend fun getReservedQuantity(variantId: String, warehouseId: String): Int {
        return repository.getReservedQuantity(variantId, warehouseId)
    }

    suspend fun getAvailableQuantity(variantId: String, warehouseId: String, totalStock: Int): Int {
        return repository.getAvailableQuantity(variantId, warehouseId, totalStock)
    }

    /**
     * Create a reservation and clean the tracking collections.
     */
    suspend fun reserve(
        variantId: String,
        warehouseId: String,
        quantity: Int,
        cartId: String? = null,
        orderId: String? = null,
        durationMinutes: Long = 15
    ): InventoryReservation {
        val freshReservation = repository.reserve(variantId, warehouseId, quantity, cartId, orderId, durationMinutes)
        putInCache(freshReservation.id, freshReservation)
        invalidateLookups(freshReservation)
        return freshReservation
    }

    /**
     * Confirm reservation -> updates database and invalidates tracking caches cleanly.
     */
    suspend fun confirmReservation(id: String, orderId: String): InventoryReservation? {
        val updated = repository.confirmReservation(id, orderId)
        if (updated != null) {
            putInCache(id, updated)
            invalidateLookups(updated)
        } else {
            removeFromCache(id)
        }
        return updated
    }

    /**
     * Cancel a specific reservation.
     */
    suspend fun cancelReservation(id: String): Boolean {
        val entity = read(id) // Get existing reference to find mapping relationships
        val wasCancelled = repository.cancelReservation(id)
        if (wasCancelled) {
            removeFromCache(id)
            entity?.let { invalidateLookups(it) }
        }
        return wasCancelled
    }

    /**
     * Cancel all active entries bound to a shopper's cart.
     */
    suspend fun cancelCartReservations(cartId: String): Int {
        val affectedRows = repository.cancelCartReservations(cartId)
        if (affectedRows > 0) {
            redis.del("${config.cacheName}:cart:$cartId")
            invalidateCollectionCaches()
        }
        return affectedRows
    }

    /**
     * Wipes collection caches completely because a batch state modification took place.
     */
    suspend fun expireOldReservations(): Int {
        val expiredCount = repository.expireOldReservations()
        if (expiredCount > 0) {
            // A sweeping change wipes global tracking matrices
            invalidateCollectionCaches()
            // Clear any variant tracking buckets since they likely contained expired data
            deleteKeysByPattern("${config.cacheName}:variant:*")
        }
        return expiredCount
    }

    /**
     * Extend an active reservation.
     */
    suspend fun extendReservation(id: String, additionalMinutes: Long = 15): InventoryReservation? {
        val updated = repository.extendReservation(id, additionalMinutes)
        if (updated != null) {
            putInCache(id, updated)
            invalidateLookups(updated)
        }
        return updated
    }

    /**
     * Eviction manager mapping target indices based on entity values.
     */
    private suspend fun invalidateLookups(reservation: InventoryReservation) {
        try {
            redis.del("${config.cacheName}:variant:${reservation.variantId}")
            reservation.cartId?.let { redis.del("${config.cacheName}:cart:$it") }
            reservation.orderId?.let { redis.del("${config.cacheName}:order:$it") }
            invalidateCollectionCaches()
        } catch (e: Exception) {
            logger.error("Error evicting lookups for reservation structural dependency ${reservation.id}", e)
        }
    }
}