package org.example.data.cache

import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import org.example.data.repo.CacheConfig
import org.example.data.repo.CrudCache
import org.example.data.repo.WishlistItemRepository
import org.example.data.repo.WishlistItemWithProduct
import org.example.domain.models.sales.WishlistItem
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@OptIn(ExperimentalLettuceCoroutinesApi::class)
class WishlistItemCache(
    private val redis: RedisCoroutinesCommands<String, String>,
    private val wishlistItemRepo: WishlistItemRepository,
    private val wishlistCache: WishlistCache, // Inter-cache dependency to invalidate parent counters
    config: CacheConfig
) : CrudCache<WishlistItem, String>(
    redis = redis,
    delegate = wishlistItemRepo,
    getId = { it.id },
    serializer = WishlistItem.serializer(),
    config = config
) {
    private val logger: Logger = LoggerFactory.getLogger(WishlistItemCache::class.java)
    private val itemListSerializer = ListSerializer(WishlistItem.serializer())
    private val itemWithProductListSerializer = ListSerializer(WishlistItemWithProduct.serializer())

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

    // --- Cached Read Extensions ---

    suspend fun findByWishlistId(wishlistId: String): List<WishlistItem> {
        val cacheKey = "${config.cacheName}:list:$wishlistId"
        return typedCacheOrFetch(cacheKey, itemListSerializer) {
            wishlistItemRepo.findByWishlistId(wishlistId)
        }
    }

    suspend fun findByWishlistIdWithProductDetails(wishlistId: String): List<WishlistItemWithProduct> {
        val cacheKey = "${config.cacheName}:details:$wishlistId"
        return typedCacheOrFetch(cacheKey, itemWithProductListSerializer) {
            wishlistItemRepo.findByWishlistIdWithProductDetails(wishlistId)
        }
    }

    suspend fun isProductInWishlist(wishlistId: String, productId: String, variantId: String? = null): Boolean {
        val cacheKey = "${config.cacheName}:exists:$wishlistId:$productId:${variantId ?: "none"}"
        return typedCacheOrFetch(cacheKey, Boolean.serializer()) {
            wishlistItemRepo.isProductInWishlist(wishlistId, productId, variantId)
        }
    }

    suspend fun addItem(wishlistId: String, productId: String, variantId: String? = null, notes: String? = null): WishlistItem? {
        val createdItem = wishlistItemRepo.addItem(wishlistId, productId, variantId, notes)
        if (createdItem != null) {
            invalidateItemCaches(wishlistId, productId, variantId)
        }
        return createdItem
    }

    suspend fun removeItem(wishlistId: String, productId: String, variantId: String? = null): Boolean {
        val removed = wishlistItemRepo.removeItem(wishlistId, productId, variantId)
        if (removed) {
            invalidateItemCaches(wishlistId, productId, variantId)
        }
        return removed
    }

    suspend fun moveToCart(wishlistItemId: String, cartId: String): Boolean {
        // Fetch historical record prior to removal so we know which parent wishlist references need cleanup
        val item = read(wishlistItemId)
        val moved = wishlistItemRepo.moveToCart(wishlistItemId, cartId)
        if (moved && item != null) {
            removeFromCache(wishlistItemId)
            invalidateItemCaches(item.wishlistId, item.productId, item.variantId)
        }
        return moved
    }

    /**
     * Purges both item tracking collections and parent wishlist aggregates.
     */
    private suspend fun invalidateItemCaches(wishlistId: String, productId: String, variantId: String?) {
        removeFromCache("${config.cacheName}:list:$wishlistId")
        removeFromCache("${config.cacheName}:details:$wishlistId")
        removeFromCache("${config.cacheName}:exists:$wishlistId:$productId:${variantId ?: "none"}")

        // Signal the parent WishlistCache to drop its aggregations and counters
        val parentWishlist = wishlistCache.read(wishlistId)
        if (parentWishlist != null) {
            wishlistCache.invalidateLookups(parentWishlist)
        } else {
            // Fallback key purge if wishlist record isn't cached
            redis.del("${wishlistCache.config.cacheName}:count:$wishlistId")
        }
    }
}
