package org.example.data.cache

import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands
import org.example.data.repo.CacheConfig
import org.example.data.repo.CrudCache
import org.example.data.repo.CustomProductQuoteRepository
import org.example.di.Component
import org.example.di.Inject
import org.example.domain.models.customization.CustomProductQuote

@OptIn(ExperimentalLettuceCoroutinesApi::class)
@Component
class CustomProductQuoteCache @Inject constructor(
    redis: RedisCoroutinesCommands<String, String>,
    private val delegate: CustomProductQuoteRepository
) : CrudCache<CustomProductQuote, String>(
    redis = redis,
    delegate = delegate,
    getId = { it.id },
    serializer = CustomProductQuote.serializer(),
    config = object : CacheConfig {
        override val cacheName: String = "customProductQuote"
        override val ttl: Long = 1800L // 30 minutes TTL
    }
) {
    // Delegate custom queries to repository
    suspend fun findByRequestId(requestId: String): List<CustomProductQuote> {
        return delegate.findByRequestId(requestId)
    }

    suspend fun getLatestQuote(requestId: String): CustomProductQuote? {
        return delegate.getLatestQuote(requestId)
    }

    suspend fun findByStatus(
        status: String,
        offset: Int = 0,
        limit: Int = 20
    ): List<CustomProductQuote> {
        return delegate.findByStatus(status, offset, limit)
    }

    suspend fun getValidQuotes(requestId: String): List<CustomProductQuote> {
        return delegate.getValidQuotes(requestId)
    }

    // Use parent's update for accept quote
    suspend fun acceptQuote(id: String): CustomProductQuote? {
        val result = delegate.acceptQuote(id)
        if (result != null) {
            putInCache(id, result)
            invalidateCollectionCaches()
        }
        return result
    }

    // Use parent's update for reject quote
    suspend fun rejectQuote(id: String, reason: String? = null): CustomProductQuote? {
        val result = delegate.rejectQuote(id, reason)
        if (result != null) {
            putInCache(id, result)
            invalidateCollectionCaches()
        }
        return result
    }

    // Use parent's update for mark as viewed
    suspend fun markAsViewed(id: String): CustomProductQuote? {
        val result = delegate.markAsViewed(id)
        if (result != null) {
            putInCache(id, result)
            invalidateCollectionCaches()
        }
        return result
    }

    suspend fun expireOldQuotes(): Int {
        val count = delegate.expireOldQuotes()
        if (count > 0) {
            invalidateCollectionCaches()
        }
        return count
    }

    suspend fun findByCreator(
        createdBy: String,
        offset: Int = 0,
        limit: Int = 20
    ): List<CustomProductQuote> {
        return delegate.findByCreator(createdBy, offset, limit)
    }

    // Override create to invalidate caches
    override suspend fun create(model: CustomProductQuote): CustomProductQuote {
        val created = super.create(model)
        invalidateCollectionCaches()
        return created
    }

    // Override update to invalidate caches
    override suspend fun update(id: String, entity: CustomProductQuote): CustomProductQuote? {
        val updated = super.update(id, entity)
        invalidateCollectionCaches()
        return updated
    }

    // Override delete to invalidate caches
    override suspend fun delete(id: String): Boolean {
        val deleted = super.delete(id)
        if (deleted) {
            invalidateCollectionCaches()
        }
        return deleted
    }
}