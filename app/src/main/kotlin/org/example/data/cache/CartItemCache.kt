package org.example.data.cache

import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands
import org.example.data.repo.CrudCache
import org.example.data.repo.CacheConfig
import org.example.data.repo.CartItemRepository
import org.example.data.repo.CartTotal
import org.example.di.Component
import org.example.di.Inject
import org.example.domain.models.sales.CartItem
import org.slf4j.LoggerFactory

@OptIn(ExperimentalLettuceCoroutinesApi::class)
@Component
class CartItemCache @Inject constructor(
    redis: RedisCoroutinesCommands<String, String>,
    private val cartItemRepository: CartItemRepository,
) : CrudCache<CartItem, String>(
    redis = redis,
    delegate = cartItemRepository,
    getId = { it.id },
    serializer = CartItem.serializer(),
    config = object : CacheConfig {
        override val cacheName: String = "cart_item"
        override val ttl: Long = 3600L
    }
) {
    private val logger = LoggerFactory.getLogger(CartItemCache::class.java)

    // Delegate custom queries to repository (these don't use standard CRUD caching)
    suspend fun findByCartId(cartId: String): List<CartItem> {
        return cartItemRepository.findByCartId(cartId)
    }

    suspend fun findByCartAndVariant(
        cartId: String,
        variantId: String,
        customizationDetails: Map<String, Any>? = null
    ): CartItem? {
        return cartItemRepository.findByCartAndVariant(cartId, variantId, customizationDetails)
    }

    // Override create to use parent's caching
    suspend fun addOrUpdateItem(
        cartId: String,
        variantId: String,
        quantity: Int,
        customizationDetails: Map<String, Any>? = null
    ): CartItem {
        val result = cartItemRepository.addOrUpdateItem(cartId, variantId, quantity, customizationDetails)
        // Cache the result using parent's putInCache
        putInCache(result.id, result)
        // Invalidate collection caches since cart contents changed
        invalidateCollectionCaches()
        return result
    }

    // Override update to handle quantity updates with caching
    suspend fun updateQuantity(id: String, quantity: Int): CartItem? {
        val result = cartItemRepository.updateQuantity(id, quantity)
        if (result != null) {
            // Use parent's caching
            putInCache(id, result)
        } else {
            // Item was deleted, remove from cache
            super.delete(id)
        }
        invalidateCollectionCaches()
        return result
    }

    // Custom queries that don't benefit from individual entity caching
    suspend fun getCartTotal(cartId: String): CartTotal? {
        return cartItemRepository.getCartTotal(cartId)
    }

    suspend fun clearCart(cartId: String): Int {
        // Get items before clearing to invalidate their caches
        val items = cartItemRepository.findByCartId(cartId)
        val result = cartItemRepository.clearCart(cartId)

        // Remove individual item caches using parent's method
        items.forEach { item ->
            super.delete(item.id)
        }
        invalidateCollectionCaches()
        return result
    }

    suspend fun isVariantInAnyCart(variantId: String): Boolean {
        return cartItemRepository.isVariantInAnyCart(variantId)
    }

    // Override delete to properly clean up
    override suspend fun delete(id: String): Boolean {
        // Get item first to know cart info for potential cache invalidation
        val item = read(id)
        val result = super.delete(id)
        if (result) {
            invalidateCollectionCaches()
        }
        return result
    }
}
