package org.example.data.cache

import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.ScanArgs
import io.lettuce.core.ScanCursor
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.builtins.ListSerializer
import org.example.data.repo.CacheConfig
import org.example.data.repo.CrudCache
import org.example.data.repo.InventoryMovementRepository
import org.example.data.repo.LowStockVariant
import org.example.data.repo.MovementSummary
import org.example.di.Component
import org.example.di.Inject
import org.example.domain.models.inventory.InventoryMovement
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.OffsetDateTime

@Component
@OptIn(ExperimentalLettuceCoroutinesApi::class)
class InventoryMovementCache @Inject constructor(
    private val redis: RedisCoroutinesCommands<String, String>,
    private val repository: InventoryMovementRepository,
    serializer: KSerializer<InventoryMovement>
) : CrudCache<InventoryMovement, String>(
    redis = redis,
    delegate = repository,
    getId = { it.id }, // Assuming your entity exposes its ULID string as .id
    serializer = serializer,
    config = object : CacheConfig {
        override val cacheName = "inventory_movements"
        override val ttl = 3600L // 1 hour default TTL for standard entries
    }
) {
    private val logger: Logger = LoggerFactory.getLogger(InventoryMovementCache::class.java)

    // Serializers for custom data objects
    private val movementSummarySerializer = MovementSummary.serializer()
    private val lowStockVariantListSerializer = ListSerializer(LowStockVariant.serializer())

    /**
     * Override creation to intercept and proactively wipe dependent stock/summary calculations.
     */
    override suspend fun create(model: InventoryMovement): InventoryMovement {
        val created = super.create(model)
        invalidateInventoryCalculations(created.variantId, created.warehouseId)
        return created
    }

    /**
     * Get movements for a specific variant with collection caching.
     */
    suspend fun findByVariantId(variantId: String, offset: Int = 0, limit: Int = 50): List<InventoryMovement> {
        val params = mapOf("variantId" to variantId)
        return readAll(offset, limit, params) // Reuses your CrudCache collection pipeline
    }

    /**
     * Get movements for a specific warehouse with collection caching.
     */
    suspend fun findByWarehouse(warehouseId: String, offset: Int = 0, limit: Int = 50): List<InventoryMovement> {
        val params = mapOf("warehouseId" to warehouseId)
        return readAll(offset, limit, params)
    }

    /**
     * Get movements filtered by type.
     */
    suspend fun findByMovementType(movementType: String, offset: Int = 0, limit: Int = 50): List<InventoryMovement> {
        val params = mapOf("movementType" to movementType)
        return readAll(offset, limit, params)
    }

    /**
     * Fetch movements matching a direct transaction reference.
     * Note: referenceId matches your structural DB type (Long or String depending on migrations)
     */
    suspend fun findByReference(referenceType: String, referenceId: Long): List<InventoryMovement> {
        // References bypass offset pagination, we generate a localized collection key manually
        val cacheKey = "${config.cacheName}:reference:$referenceType:$referenceId"
        val cachedJson = redis.get(cacheKey)

        if (cachedJson != null) {
            try {
                return Json.decodeFromString(ListSerializer(InventoryMovement.serializer()), cachedJson)
            } catch (e: Exception) {
                logger.error("Failed to deserialize reference cache for key: $cacheKey", e)
            }
        }

        val entities = repository.findByReference(referenceType, referenceId)
        if (entities.isNotEmpty()) {
            try {
                redis.setex(cacheKey, config.ttl ?: 3600L, Json.encodeToString(ListSerializer(InventoryMovement.serializer()), entities))
            } catch (e: Exception) {
                logger.error("Failed to write reference cache for key: $cacheKey", e)
            }
        }
        return entities
    }

    /**
     * Caches current stock level calculations using atomic string variables.
     */
    suspend fun getCurrentStock(variantId: String, warehouseId: String): Int {
        val cacheKey = "${config.cacheName}:stock:$warehouseId:$variantId"
        val cachedStock = redis.get(cacheKey)

        if (cachedStock != null) {
            return cachedStock.toIntOrNull() ?: 0
        }

        val freshStock = repository.getCurrentStock(variantId, warehouseId)
        try {
            // Keep stock counts shorter-lived to prevent drift from unsynced application mutations
            redis.setex(cacheKey, 300L, freshStock.toString()) // 5 minute safety TTL
        } catch (e: Exception) {
            logger.error("Failed to cache stock level for key: $cacheKey", e)
        }
        return freshStock
    }

    /**
     * Caches complex date range summaries.
     */
    suspend fun getMovementSummary(startDate: OffsetDateTime, endDate: OffsetDateTime, warehouseId: String? = null): MovementSummary {
        val cacheKey = "${config.cacheName}:summary:${warehouseId ?: "global"}:${startDate.toEpochSecond()}:${endDate.toEpochSecond()}"
        val cachedJson = redis.get(cacheKey)

        if (cachedJson != null) {
            try {
                return Json.decodeFromString(movementSummarySerializer, cachedJson)
            } catch (e: Exception) {
                logger.error("Failed to deserialize movement summary cache", e)
            }
        }

        val freshSummary = repository.getMovementSummary(startDate, endDate, warehouseId)
        try {
            redis.setex(cacheKey, 600L, Json.encodeToString(movementSummarySerializer, freshSummary)) // 10 min TTL
        } catch (e: Exception) {
            logger.error("Failed to cache movement summary", e)
        }
        return freshSummary
    }

    /**
     * Caches low stock variants array list.
     */
    suspend fun getLowStockVariants(warehouseId: String, threshold: Int = 10): List<LowStockVariant> {
        val cacheKey = "${config.cacheName}:low-stock:$warehouseId:$threshold"
        val cachedJson = redis.get(cacheKey)

        if (cachedJson != null) {
            try {
                return Json.decodeFromString(lowStockVariantListSerializer, cachedJson)
            } catch (e: Exception) {
                logger.error("Failed to deserialize low stock variants cache", e)
            }
        }

        val freshVariants = repository.getLowStockVariants(warehouseId, threshold)
        try {
            redis.setex(cacheKey, 600L, Json.encodeToString(lowStockVariantListSerializer, freshVariants))
        } catch (e: Exception) {
            logger.error("Failed to cache low stock variants", e)
        }
        return freshVariants
    }

    /**
     * Bypasses cache check entirely for runtime validations to prevent race conditions.
     */
    suspend fun validateMovement(movement: InventoryMovement): Boolean {
        return repository.validateMovement(movement)
    }

    /**
     * Private internal cleaner to targetedly clear calculation keys when stock shifts.
     */
    private suspend fun invalidateInventoryCalculations(variantId: String, warehouseId: String) {
        try {
            // 1. Wipe specific stock counter
            redis.del("${config.cacheName}:stock:$warehouseId:$variantId")

            // 2. Wipe low stock thresholds and generic date summaries for that warehouse
            invalidateCollectionCaches() // handles standard list patterns

            // For highly volatile keys, scan and wipe structural summaries matching the warehouse footprint
            val scanArgs = ScanArgs.Builder.matches("${config.cacheName}:summary:$warehouseId:*").limit(50)
            var cursor = redis.scan(ScanCursor.INITIAL, scanArgs)
            while (cursor != null) {
                if (cursor.keys.isNotEmpty()) {
                    redis.del(*cursor.keys.toTypedArray())
                }
                if (cursor.isFinished) break
                cursor = redis.scan(cursor, scanArgs)
            }
        } catch (e: Exception) {
            logger.error("Error performing contextual cache invalidation for variant $variantId", e)
        }
    }
}