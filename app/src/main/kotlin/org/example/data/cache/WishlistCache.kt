package org.example.data.cache

import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.example.data.repo.CacheConfig
import org.example.data.repo.CrudCache
import org.example.data.repo.WishlistRepository
import org.example.data.repo.WishlistWithItemCount
import org.example.di.Inject
import org.example.di.Injectable
import org.example.domain.models.sales.Wishlist
import org.example.plugins.nullable
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@OptIn(ExperimentalLettuceCoroutinesApi::class)
@Injectable
class WishlistCache @Inject constructor(
    private val redis: RedisCoroutinesCommands<String, String>,
    private val wishlistRepo: WishlistRepository,
) : CrudCache<Wishlist, String>(
    redis = redis,
    delegate = wishlistRepo,
    getId = { it.id },
    serializer = Wishlist.serializer(),
    config = object : CacheConfig {
        override val cacheName: String = "wishlist"
        override val ttl: Long = 1800L
    }
) {
    private val logger: Logger = LoggerFactory.getLogger(WishlistCache::class.java)
    private val wishlistListSerializer = ListSerializer(Wishlist.serializer())

    private suspend fun <T> typedCacheOrFetch(
        cacheKey: String,
        serializer: kotlinx.serialization.KSerializer<T>,
        fetcher: suspend () -> T
    ): T {
        val cachedJson = try { redis.get(cacheKey) } catch (e: Exception) { null }
        if (cachedJson != null) {
            try { return Json.decodeFromString(serializer, cachedJson) } catch (e: Exception) { }
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
                if (config.ttl != null) redis.setex(cacheKey, config.ttl!!, jsonValue) else redis.set(cacheKey, jsonValue)
            } catch (e: Exception) { }
        }
        return result
    }

    override suspend fun create(model: Wishlist): Wishlist {
        val created = super.create(model)
        invalidateLookups(created)
        return created
    }

    override suspend fun update(id: String, entity: Wishlist): Wishlist? {
        val updated = super.update(id, entity)
        if (updated != null) invalidateLookups(updated)
        return updated
    }

    override suspend fun delete(id: String): Boolean {
        val existing = read(id)
        val deleted = super.delete(id)
        if (deleted && existing != null) invalidateLookups(existing)
        return deleted
    }

    // --- Cached Read Extensions ---

    suspend fun findByUserId(userId: String): List<Wishlist> {
        val cacheKey = "${config.cacheName}:user:$userId"
        return typedCacheOrFetch(cacheKey, wishlistListSerializer) {
            wishlistRepo.findByUserId(userId)
        }
    }

    suspend fun findDefaultByUserId(userId: String): Wishlist? {
        val cacheKey = "${config.cacheName}:default:$userId"
        return typedCacheOrFetch(cacheKey, Wishlist.serializer().nullable) {
            wishlistRepo.findDefaultByUserId(userId)
        }
    }

    suspend fun getOrCreateDefault(userId: String): Wishlist {
        // Leverages findDefaultByUserId under the hood, allowing read hits to hit the cache pipeline safely
        return findDefaultByUserId(userId) ?: create(Wishlist(userId = userId, name = "default"))
    }

    suspend fun findPublicWishlists(offset: Int = 0, limit: Int = 20): List<Wishlist> {
        val cacheKey = "${config.cacheName}:public:$offset:$limit"
        return typedCacheOrFetch(cacheKey, wishlistListSerializer) {
            wishlistRepo.findPublicWishlists(offset, limit)
        }
    }

    suspend fun getWishlistWithItemCount(wishlistId: String): WishlistWithItemCount? {
        val cacheKey = "${config.cacheName}:count:$wishlistId"
        return typedCacheOrFetch(cacheKey, WishlistWithItemCount.serializer().nullable) {
            wishlistRepo.getWishlistWithItemCount(wishlistId)
        }
    }


    suspend fun invalidateLookups(wishlist: Wishlist) {
        invalidateCollectionCaches()
        removeFromCache("${config.cacheName}:user:${wishlist.userId}")
        removeFromCache("${config.cacheName}:default:${wishlist.userId}")
        removeFromCache("${config.cacheName}:count:${wishlist.id}")
        deleteKeysByPattern("${config.cacheName}:public:*")
    }
}