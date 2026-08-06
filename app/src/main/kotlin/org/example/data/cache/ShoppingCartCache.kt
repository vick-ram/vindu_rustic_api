package org.example.data.cache

import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands
import kotlinx.serialization.json.Json
import org.example.data.repo.CacheConfig
import org.example.data.repo.ShoppingCartRepository
import org.example.data.repo.CartWithItemCount
import org.example.data.repo.CrudCache
import org.example.di.Inject
import org.example.di.Injectable
import org.example.domain.models.sales.ShoppingCart
import org.example.plugins.nullable
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.UUID
import kotlin.uuid.toKotlinUuid

@OptIn(ExperimentalLettuceCoroutinesApi::class)
@Injectable
class ShoppingCartCache @Inject constructor(
    private val redis: RedisCoroutinesCommands<String, String>,
    private val cartRepo: ShoppingCartRepository,
) : CrudCache<ShoppingCart, String>(
    redis = redis,
    delegate = cartRepo,
    getId = { it.id },
    serializer = ShoppingCart.serializer(),
    config = object : CacheConfig {
        override val cacheName: String = "shopping_cart_cache"
        override val ttl: Long = 3600L
    }
) {
    private val logger: Logger = LoggerFactory.getLogger(ShoppingCartCache::class.java)

    /**
     * Internal read-through helper to minimize boilerplate logic.
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
        if (result != null) {
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

    override suspend fun create(model: ShoppingCart): ShoppingCart {
        val created = super.create(model)
        invalidateLookups(created)
        return created
    }

    override suspend fun update(id: String, entity: ShoppingCart): ShoppingCart? {
        val updated = super.update(id, entity)
        if (updated != null) {
            invalidateLookups(updated)
        }
        return updated
    }

    override suspend fun delete(id: String): Boolean {
        // Fetch the object immediately prior to removal so we know which indexes to clean up
        val existing = read(id)
        val deleted = super.delete(id)
        if (deleted && existing != null) {
            invalidateLookups(existing)
        }
        return deleted
    }

    // --- Domain Read Queries (Cached) ---

    suspend fun findByUserId(userId: String): ShoppingCart? {
        val cacheKey = "${config.cacheName}:user:$userId"
        return typedCacheOrFetch(cacheKey, ShoppingCart.serializer().nullable) {
            cartRepo.findByUserId(userId)
        }
    }

    suspend fun findByGuestToken(guestToken: UUID): ShoppingCart? {
        val cacheKey = "${config.cacheName}:guest:$guestToken"
        return typedCacheOrFetch(cacheKey, ShoppingCart.serializer().nullable) {
            cartRepo.findByGuestToken(guestToken.toKotlinUuid())
        }
    }

    suspend fun getCartWithItemCount(cartId: String): CartWithItemCount? {
        val cacheKey = "${config.cacheName}:count:$cartId"
        return typedCacheOrFetch(cacheKey, CartWithItemCount.serializer().nullable) {
            cartRepo.getCartWithItemCount(cartId)
        }
    }

    suspend fun getOrCreateForUser(userId: String): ShoppingCart {
        // Leverages read-through caching pipeline
        return findByUserId(userId) ?: create(ShoppingCart(userId = userId))
    }

    suspend fun getOrCreateForGuest(guestToken: UUID): ShoppingCart {
        // Leverages read-through caching pipeline
        return findByGuestToken(guestToken) ?: create(ShoppingCart(guestToken = guestToken.toKotlinUuid()))
    }

    suspend fun mergeCarts(guestToken: UUID, userId: String): ShoppingCart {
        // Run database transaction updates
        val finalUserCart = cartRepo.mergeCarts(guestToken.toKotlinUuid(), userId)

        // Clear all tracking caches related to both tokens to protect state updates
        removeFromCache("${config.cacheName}:guest:$guestToken")
        removeFromCache("${config.cacheName}:user:$userId")
        removeFromCache(finalUserCart.id)
        removeFromCache("${config.cacheName}:count:${finalUserCart.id}")

        // Evict any stale collections if applicable
        invalidateCollectionCaches()

        return finalUserCart
    }

    suspend fun touchCart(id: String): Boolean {
        val touched = cartRepo.touchCart(id)
        if (touched) {
            // Touch implicitly shifts temporal rules; get the target record and purge downstream lookups
            val current = read(id)
            removeFromCache(id)
            removeFromCache("${config.cacheName}:count:$id")
            if (current != null) {
                invalidateLookups(current)
            }
        }
        return touched
    }

    suspend fun deleteAbandonedCarts(olderThanDays: Int): Int {
        val deletedCount = cartRepo.deleteAbandonedCarts(olderThanDays)
        if (deletedCount > 0) {
            // Because specific guest keys are hard to extract directly out of raw sweep queries,
            // sweeping out generic dynamic ranges or clearing the structural collection patterns ensures safety.
            invalidateCollectionCaches()
            deleteKeysByPattern("${config.cacheName}:guest:*")
        }
        return deletedCount
    }

    /**
     * Invalidates targeted side indexes associated with the active domain entity.
     */
    private suspend fun invalidateLookups(cart: ShoppingCart) {
        invalidateCollectionCaches()
        removeFromCache("${config.cacheName}:count:${cart.id}")
        cart.userId?.let { removeFromCache("${config.cacheName}:user:$it") }
        removeFromCache("${config.cacheName}:guest:${cart.guestToken}")
    }
}