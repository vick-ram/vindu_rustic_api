package org.example.data.repo

import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands
import org.example.domain.repo.CrudRepository
import org.example.utils.GsonFactory
import org.example.utils.RedisService
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class CrudCache<T: Any, ID: Any>(
    private val delegate: CrudRepository<T, ID>,
    private val clazz: Class<T>,
    private val getId: (T) -> ID,
    private val cacheName: String? = null,
    private val logger: Logger = LoggerFactory.getLogger(CrudCache::class.java),
    private val ttl: Long? = null
): CrudRepository<T, ID> {

    private val gson = GsonFactory.gson

    private fun generateCacheKey(id: ID): String {
        return "$cacheName:$id"
    }

    @OptIn(ExperimentalLettuceCoroutinesApi::class)
    private suspend fun <R> withRedis(block: suspend (RedisCoroutinesCommands<String, String>) -> R): R {
        return try {
            block(RedisService.commands)
        } catch (e: Exception) {
            logger.error("Redis operation failed", e)
            throw e
        }
    }

    @OptIn(ExperimentalLettuceCoroutinesApi::class)
    private suspend fun putInCache(id: ID, entity: T) {
        withRedis { commands ->
            val key = generateCacheKey(id)
            val jsonValue = gson.toJson(entity)
            if (ttl != null) {
                commands.setex(key, ttl, jsonValue)
            } else {
                commands.set(key, jsonValue)
            }
        }
    }

    @OptIn(ExperimentalLettuceCoroutinesApi::class)
    private suspend fun getFromCache(id: ID): T? {
        return withRedis { commands ->
            val key = generateCacheKey(id)
            val jsonValue = commands.get(key)
            jsonValue?.let {
                try {
                    gson.fromJson(it, clazz)
                } catch (e: Exception) {
                    logger.error("Failed to deserialize cached entity for ID: $id", e)
                    null
                }
            }
        }
    }

    @OptIn(ExperimentalLettuceCoroutinesApi::class)
    private suspend fun removeFromCache(id: ID) {
        withRedis { commands ->
            val key = generateCacheKey(id)
            commands.del(key)
        }
    }

    override suspend fun create(entity: T): T {
        val created = delegate.create(entity)
        val id = getId(created)
        putInCache(id, created)
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

    override suspend fun readAll(
        offset: Int,
        limit: Int,
        queryParams: Map<String, String>?
    ): List<T> {
        // For collections, we typically don't cache them in Redis due to complexity
        // You could implement pattern-based caching if needed
        return delegate.readAll(offset, limit, queryParams)
    }

    override suspend fun update(id: ID, entity: T): T? {
        val updated = delegate.update(id, entity)
        if (updated != null) {
            putInCache(id, updated)
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
        return deleted
    }

    @OptIn(ExperimentalLettuceCoroutinesApi::class)
    suspend fun clearCache() {
        withRedis { commands ->
            // This is a simple approach - for production, you might want pattern matching
            logger.info("Clearing Redis cache for pattern: $cacheName:*")
            // Note: Redis doesn't have a direct pattern delete in single command
            // You might need to use SCAN + DEL in production

        }
    }
}