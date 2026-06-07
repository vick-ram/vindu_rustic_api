package org.example.data.repo

import com.google.gson.reflect.TypeToken
import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.KeyScanCursor
import io.lettuce.core.ScanArgs
import io.lettuce.core.ScanCursor
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands
import org.example.domain.repo.CrudRepository
import org.example.utils.Json
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class CrudCache<T: Any, ID: Any> @OptIn(ExperimentalLettuceCoroutinesApi::class) constructor(
    private val redis: RedisCoroutinesCommands<String, String>,
    private val delegate: CrudRepository<T, ID>,
    private val clazz: Class<T>,
    private val getId: (T) -> ID,
    private val cacheName: String? = null,
    private val logger: Logger = LoggerFactory.getLogger(CrudCache::class.java),
    private val ttl: Long? = null
): CrudRepository<T, ID> {

    private fun generateCacheKey(id: ID): String {
        return "$cacheName:$id"
    }

    private fun generateCollectionCacheKey(queryParams: Map<String, String>?, offset: Int, limit: Int): String {
        val paramsHash = queryParams?.entries
            ?.sortedBy { it.key }
            ?.joinToString("&") { "${it.key}=${it.value}" } ?: ""
        return "$cacheName:collection:$paramsHash:$offset:$limit"
    }

    @OptIn(ExperimentalLettuceCoroutinesApi::class)
    private suspend fun putInCache(id: ID, entity: T) {
            val key = generateCacheKey(id)
            val jsonValue = Json.encodeToString(entity)
            if (ttl != null) {
                redis.setex(key, ttl, jsonValue)
            } else {
                redis.set(key, jsonValue)
            }
    }

    @OptIn(ExperimentalLettuceCoroutinesApi::class)
    private suspend fun getFromCache(id: ID): T? {
            val key = generateCacheKey(id)
            val jsonValue = redis.get(key)
        return jsonValue?.let { deserializeEntity(it, id.toString()) }
    }

    @OptIn(ExperimentalLettuceCoroutinesApi::class)
    private suspend fun removeFromCache(id: ID) {
            val key = generateCacheKey(id)
            redis.del(key)
    }

    private fun deserializeEntity(jsonValue: String, id: String): T? {
        return try {
            Json.decodeFromString(jsonValue, clazz)
        } catch (e: Exception) {
            logger.error("Failed to deserialize cached entity for ID: $id", e)
            null
        }
    }

    override suspend fun create(entity: T): T {
        val created = delegate.create(entity)
        val id = getId(created)
        putInCache(id, created)
        invalidateCollectionCaches()
        return created
    }

    override suspend fun read(id: ID): T? {
        val cached = getFromCache(id)
        if (cached != null) {
            return cached
        }
        val entity = delegate.read(id)
        if (entity != null) {
            putInCache(id, entity)
        }
        return entity
    }

    @OptIn(ExperimentalLettuceCoroutinesApi::class)
    override suspend fun readAll(
        offset: Int,
        limit: Int,
        queryParams: Map<String, String>?
    ): List<T> {
        val cacheKey = generateCollectionCacheKey(queryParams, offset, limit)

        // Try to get from cache first
        val cachedJson = redis.get(cacheKey)
        if (cachedJson != null) {
            try {
                val listType = TypeToken.getParameterized(List::class.java, clazz).type
                return Json.decodeFromString( cachedJson, listType)
            } catch (e: Exception) {
                logger.error("Failed to deserialize cached collection for key: $cacheKey", e)
                // Fall through to fetch from delegate
            }
        }

        // Fetch from delegate
        val entities = delegate.readAll(offset, limit, queryParams)

        // Cache the result
        if (entities.isNotEmpty()) {
            try {
                val jsonValue = Json.encodeToString(entities)
                if (ttl != null) {
                    redis.setex(cacheKey, ttl, jsonValue)
                } else {
                    redis.set(cacheKey, jsonValue)
                }
            } catch (e: Exception) {
                logger.error("Failed to cache collection for key: $cacheKey", e)
            }
        }

        return entities
    }

    override suspend fun update(id: ID, entity: T): T? {
        val updated = delegate.update(id, entity)
        if (updated != null) {
            putInCache(id, updated)
            invalidateCollectionCaches()
        } else {
            removeFromCache(id)
        }
        return updated
    }

    override suspend fun delete(id: ID): Boolean {
        val deleted = delegate.delete(id)
        if (deleted) {
            removeFromCache(id)
        }
        invalidateCollectionCaches()
        return deleted
    }

    @OptIn(ExperimentalLettuceCoroutinesApi::class)
    suspend fun clearCache() {
        logger.info("Clearing Redis cache for pattern: $cacheName:*")
        deleteKeysByPattern("$cacheName:*")
    }

    @OptIn(ExperimentalLettuceCoroutinesApi::class)
    private suspend fun invalidateCollectionCaches() {
        deleteKeysByPattern("$cacheName:collection:*")
    }

    @OptIn(ExperimentalLettuceCoroutinesApi::class)
    private suspend fun deleteKeysByPattern(pattern: String) {
        try {
            val scanArgs = ScanArgs.Builder.matches(pattern).limit(100)
            var scanCursor: KeyScanCursor<String>? = redis.scan(ScanCursor.INITIAL, scanArgs)
            var totalDeleted = 0

            while (scanCursor != null && !scanCursor.isFinished) {
                val keys = scanCursor.keys
                if (keys.isNotEmpty()) {
                    redis.del(*keys.toTypedArray())
                    totalDeleted += keys.size
                    logger.debug("Deleted batch of ${keys.size} keys matching pattern: $pattern")
                }
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