package org.example.data.repo

import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.KeyScanCursor
import io.lettuce.core.ScanArgs
import io.lettuce.core.ScanCursor
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.slf4j.Logger
import org.slf4j.LoggerFactory

interface CacheConfig {
    val cacheName: String
    val ttl: Long?
}

@OptIn(ExperimentalLettuceCoroutinesApi::class)
open class CrudCache<Model: Any, ID: Any>(
    private val redis: RedisCoroutinesCommands<String, String>,
    private val delegate: CrudRepository<Model, ID>,
    private val getId: (Model) -> ID,
    private val serializer: KSerializer<Model>,
    val config: CacheConfig
) {

    private val logger: Logger = LoggerFactory.getLogger(CrudCache::class.java)

    private val collectionSerializer = ListSerializer(serializer)

    private fun generateCacheKey(id: ID): String {
        return "${config.cacheName}:$id"
    }

    private fun generateCollectionCacheKey(queryParams: Map<String, String>?, offset: Int, limit: Int): String {
        val paramsHash = queryParams?.entries
            ?.sortedBy { it.key }
            ?.joinToString("&") { "${it.key}=${it.value}" } ?: ""
        return "${config.cacheName}:collection:$paramsHash:$offset:$limit"
    }

    @OptIn(ExperimentalLettuceCoroutinesApi::class)
    suspend fun putInCache(id: ID, entity: Model) {
        try {
            val key = generateCacheKey(id)
            val jsonValue = Json.encodeToString(serializer, entity)
            if (config.ttl != null) {
                redis.setex(key, config.ttl!!, jsonValue)
            } else {
                redis.set(key, jsonValue)
            }
        } catch (e: Exception) {
            logger.error("Failed to cache entity with ID: $id", e)
        }
    }

    @OptIn(ExperimentalLettuceCoroutinesApi::class)
    private suspend fun getFromCache(id: ID): Model? {
        return try {
            val key = generateCacheKey(id)
            val jsonValue = redis.get(key)
            jsonValue?.let { Json.decodeFromString(serializer, it) }
        } catch (e: Exception) {
            logger.error("Failed to get entity from cache for ID: $id", e)
            null
        }
    }

    @OptIn(ExperimentalLettuceCoroutinesApi::class)
    private suspend fun removeFromCache(id: ID) {
        try {
            val key = generateCacheKey(id)
            redis.del(key)
        } catch (e: Exception) {
            logger.error("Failed to remove entity from cache for ID: $id", e)
        }
    }

    open suspend fun create(model: Model): Model {
        val created = delegate.create(model)
        val id = getId(created)
        putInCache(id, created)
        invalidateCollectionCaches()
        return created
    }

    suspend fun read(id: ID): Model? {
        return getFromCache(id) ?: delegate.read(id)?.also { entity ->
            putInCache(id, entity)
        }
    }

    @OptIn(ExperimentalLettuceCoroutinesApi::class)
    suspend fun readAll(
        offset: Int,
        limit: Int,
        queryParams: Map<String, String>?
    ): List<Model> {
        val cacheKey = generateCollectionCacheKey(queryParams, offset, limit)

        // Try cache first
        val cachedJson = redis.get(cacheKey)
        if (cachedJson != null) {
            try {
                return Json.decodeFromString(collectionSerializer, cachedJson)
            } catch (e: Exception) {
                logger.error("Failed to deserialize cached collection for key: $cacheKey", e)
            }
        }

        // Fetch from delegate
        val entities = delegate.readAll(offset, limit, queryParams)

        // Cache non-empty results
        if (entities.isNotEmpty()) {
            try {
                val jsonValue = Json.encodeToString(collectionSerializer, entities)
                if (config.ttl != null) {
                    redis.setex(cacheKey, config.ttl!!, jsonValue)
                } else {
                    redis.set(cacheKey, jsonValue)
                }
            } catch (e: Exception) {
                logger.error("Failed to cache collection for key: $cacheKey", e)
            }
        }

        return entities
    }

    open suspend fun update(id: ID, entity: Model): Model? {
        val updated = delegate.update(id, entity)
        if (updated != null) {
            putInCache(id, updated)
        } else {
            removeFromCache(id)
        }
        invalidateCollectionCaches()
        return updated
    }

    open suspend fun delete(id: ID): Boolean {
        val deleted = delegate.delete(id)
        if (deleted) {
            removeFromCache(id)
            invalidateCollectionCaches()
        }
        return deleted
    }

    @OptIn(ExperimentalLettuceCoroutinesApi::class)
    suspend fun clearCache() {
        logger.info("Clearing Redis cache for pattern: ${config.cacheName}:*")
        deleteKeysByPattern("${config.cacheName}:*")
    }

    @OptIn(ExperimentalLettuceCoroutinesApi::class)
    suspend fun invalidateCollectionCaches() {
        deleteKeysByPattern("${config.cacheName}:collection:*")
    }

    @OptIn(ExperimentalLettuceCoroutinesApi::class)
    private suspend fun deleteKeysByPattern(pattern: String) {
        try {
            val scanArgs = ScanArgs.Builder.matches(pattern).limit(100)
            var scanCursor: KeyScanCursor<String>? = redis.scan(ScanCursor.INITIAL, scanArgs)
            var totalDeleted = 0

            while (scanCursor != null) {
                val keys = scanCursor.keys
                if (keys.isNotEmpty()) {
                    redis.del(*keys.toTypedArray())
                    totalDeleted += keys.size
                    logger.debug("Deleted batch of ${keys.size} keys matching pattern: $pattern")
                }
                if (scanCursor.isFinished) break

                scanCursor = redis.scan(scanCursor, scanArgs)
            }

            if (totalDeleted > 0) {
                logger.info("Deleted $totalDeleted keys matching pattern: $pattern")
            }
        } catch (e: Exception) {
            logger.error("Failed to delete keys by pattern: $pattern", e)
            // Don't rethrow - cache operations shouldn't break the main flow
        }
    }
}
