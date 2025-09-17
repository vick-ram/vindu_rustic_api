package org.example.data.repo

import org.ehcache.Cache
import org.ehcache.config.builders.CacheConfigurationBuilder
import org.ehcache.config.builders.CacheManagerBuilder
import org.ehcache.config.builders.ResourcePoolsBuilder
import org.ehcache.config.units.EntryUnit
import org.ehcache.config.units.MemoryUnit
import org.ehcache.impl.config.persistence.CacheManagerPersistenceConfiguration
import org.example.domain.repo.CrudRepository
import org.example.utils.cachePath
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File

class CrudCache<T: Any, ID: Any>(
    private val delegate: CrudRepository<T, ID>,
    clazz: Class<T>,               // for cache value type
    idClazz: Class<ID>,
    storageFile: File?,
    private val getId: (T) -> ID,
    cacheName: String? = null,
    private val logger: Logger = LoggerFactory.getLogger(CrudCache::class.java)
): CrudRepository<T, ID> {
    private val uniquePath = cachePath(storageFile)
    private val cacheManager = CacheManagerBuilder.newCacheManagerBuilder()
        .with(CacheManagerPersistenceConfiguration(uniquePath))
        .withCache(
            cacheName,
            CacheConfigurationBuilder.newCacheConfigurationBuilder(
                idClazz,
                clazz,
                ResourcePoolsBuilder.newResourcePoolsBuilder()
                    .heap(1000, EntryUnit.ENTRIES)
                    .offheap(10, MemoryUnit.MB)
                    .disk(100, MemoryUnit.MB)
            )
        ).build(true)

    private val cache: Cache<ID, T> = cacheManager.getCache("crud-cache", idClazz, clazz)

    override suspend fun create(entity: T): T {
        val created = delegate.create(entity)
        val id = getId(created)
        cache.put(id, created)
        return created
    }

    override suspend fun read(id: ID): T? {
        logger.info("READ - Checking cache for ID: $id")
        val cached = cache.get(id)
        if (cached != null) {
            logger.info("READ - Cache HIT for ID: $id")
            return cached
        }
        logger.info("READ - Cache MISS for ID: $id, delegating to repository")
        val entity = delegate.read(id)
        if (entity != null) cache.put(id, entity)
        return entity
    }

    override suspend fun readAll(
        offset: Int,
        limit: Int,
        queryParams: Map<String, String>?
    ): List<T> {
        return delegate.readAll(offset, limit, queryParams)
    }

    override suspend fun update(id: ID, entity: T): T? {
        val updated = delegate.update(id, entity)
        if (updated != null) cache.put(id, updated)
        return updated
    }

    override suspend fun delete(id: ID): Boolean {
        val deleted = delegate.delete(id)
        if (deleted) cache.remove(id)
        return deleted
    }
}