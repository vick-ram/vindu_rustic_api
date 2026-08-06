package org.example.data.cache

import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.example.data.repo.CacheConfig
import org.example.data.repo.CrudCache
import org.example.data.repo.WarehouseRepository
import org.example.data.repo.WarehouseStockSummary
import org.example.data.repo.WarehouseWithAddress
import org.example.di.Injectable
import org.example.domain.models.inventory.Warehouse
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@OptIn(ExperimentalLettuceCoroutinesApi::class)
@Injectable
class WarehouseCache(
    private val redis: RedisCoroutinesCommands<String, String>,
    private val warehouseRepo: WarehouseRepository,
) : CrudCache<Warehouse, String>(
    redis = redis,
    delegate = warehouseRepo,
    getId = { it.id },
    serializer = Warehouse.serializer(),
    config = object : CacheConfig {
        override val cacheName: String = "warehouse"
        override val ttl: Long = 3600L
    }
) {
    private val logger: Logger = LoggerFactory.getLogger(WarehouseCache::class.java)
    private val warehouseListSerializer = ListSerializer(Warehouse.serializer())

    /**
     * Shared read-through pipeline wrapper.
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

    // --- Overridden Mutations ---

    override suspend fun create(model: Warehouse): Warehouse {
        val created = super.create(model)
        invalidateLookups(created)
        return created
    }

    override suspend fun update(id: String, entity: Warehouse): Warehouse? {
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

    suspend fun findActive(): List<Warehouse> {
        val cacheKey = "${config.cacheName}:active"
        return typedCacheOrFetch(cacheKey, warehouseListSerializer) {
            warehouseRepo.findActive()
        }
    }

    suspend fun searchByName(query: String, includeInactive: Boolean = false): List<Warehouse> {
        // Dynamic search parameter tokens combined into the cache matrix key
        val cacheKey = "${config.cacheName}:search:${query.lowercase().trim()}:$includeInactive"
        return typedCacheOrFetch(cacheKey, warehouseListSerializer) {
            warehouseRepo.searchByName(query, includeInactive)
        }
    }

    suspend fun findWithAddress(warehouseId: String): WarehouseWithAddress? {
        val cacheKey = "${config.cacheName}:address:$warehouseId"
        return typedCacheOrFetch(cacheKey, WarehouseWithAddress.serializer().nullable) {
            warehouseRepo.findWithAddress(warehouseId)
        }
    }

    suspend fun getWarehouseStockSummary(warehouseId: String): WarehouseStockSummary {
        val cacheKey = "${config.cacheName}:summary:$warehouseId"
        return typedCacheOrFetch(cacheKey, WarehouseStockSummary.serializer()) {
            warehouseRepo.getWarehouseStockSummary(warehouseId)
        }
    }

    // --- Uncached Checks ---

    suspend fun isNameUnique(name: String, excludeId: String? = null): Boolean {
        // Direct pass-through to avoid caching transactional validation data
        return warehouseRepo.isNameUnique(name, excludeId)
    }

    // --- Targeted Domain Updates ---

    suspend fun activate(id: String): Warehouse? {
        val updated = warehouseRepo.activate(id)
        if (updated != null) {
            removeFromCache(id)
            invalidateLookups(updated)
        }
        return updated
    }

    suspend fun deactivate(id: String): Warehouse? {
        val updated = warehouseRepo.deactivate(id)
        if (updated != null) {
            removeFromCache(id)
            invalidateLookups(updated)
        }
        return updated
    }

    // --- Multi-Index Invalidation Strategy ---

    /**
     * Evicts structural indexing pools whenever warehouse details change.
     */
    private suspend fun invalidateLookups(warehouse: Warehouse) {
        invalidateCollectionCaches()
        removeFromCache("${config.cacheName}:active")
        removeFromCache("${config.cacheName}:address:${warehouse.id}")
        removeFromCache("${config.cacheName}:summary:${warehouse.id}")

        // Wipe wildcards for arbitrary search queries
        deleteKeysByPattern("${config.cacheName}:search:*")
    }
}

/**
 * Utility handling compiler requirements for polymorphic serialization bounds.
 */
private val <T> kotlinx.serialization.KSerializer<T>.nullable: kotlinx.serialization.KSerializer<T?>
    get() = @Suppress("UNCHECKED_CAST") (this as kotlinx.serialization.KSerializer<T?>)