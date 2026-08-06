package org.example.services

import org.example.data.cache.ShoppingCartCache
import org.example.data.repo.CartWithItemCount
import org.example.di.Inject
import org.example.di.Injectable
import org.example.domain.models.sales.ShoppingCart
import org.slf4j.LoggerFactory
import java.time.OffsetDateTime
import java.util.UUID

@Injectable
class ShoppingCartService @Inject constructor(
    private val cartCache: ShoppingCartCache
) {
    private val logger = LoggerFactory.getLogger(ShoppingCartService::class.java)

    /**
     * Get or create a cart for an authenticated user
     */
    suspend fun getUserCart(userId: String): ShoppingCart {
        return cartCache.getOrCreateForUser(userId)
    }

    /**
     * Get or create a cart for a guest user
     */
    suspend fun getGuestCart(guestToken: UUID): ShoppingCart {
        return cartCache.getOrCreateForGuest(guestToken)
    }

    /**
     * Get a specific cart by ID
     */
    suspend fun getCart(cartId: String): ShoppingCart? {
        return cartCache.read(cartId)
    }

    /**
     * Get cart with item count for display purposes
     */
    suspend fun getCartWithItemCount(cartId: String): CartWithItemCount? {
        return cartCache.getCartWithItemCount(cartId)
    }

    /**
     * Merge a guest cart into a user cart when guest logs in
     */
    suspend fun mergeGuestCartToUser(guestToken: UUID, userId: String): ShoppingCart {
        return cartCache.mergeCarts(guestToken, userId)
    }

    /**
     * Update an existing cart
     */
    suspend fun updateCart(cartId: String, cart: ShoppingCart): ShoppingCart? {
        return cartCache.update(cartId, cart)
    }

    /**
     * Delete a specific cart
     */
    suspend fun deleteCart(cartId: String): Boolean {
        return cartCache.delete(cartId)
    }

    /**
     * Touch a cart to update its last modified timestamp
     */
    suspend fun touchCart(cartId: String): Boolean {
        return cartCache.touchCart(cartId)
    }

    /**
     * Clean up abandoned carts older than specified days
     */
    suspend fun cleanupAbandonedCarts(olderThanDays: Int = 30): Int {
        return cartCache.deleteAbandonedCarts(olderThanDays)
    }

    /**
     * Find a cart by user ID
     */
    suspend fun findCartByUserId(userId: String): ShoppingCart? {
        return cartCache.findByUserId(userId)
    }

    /**
     * Find a cart by guest token
     */
    suspend fun findCartByGuestToken(guestToken: UUID): ShoppingCart? {
        return cartCache.findByGuestToken(guestToken)
    }

    /**
     * Transfer cart ownership from guest to user
     */
    suspend fun transferCartOwnership(guestToken: UUID, userId: String): ShoppingCart {
        return try {

            val guestCart = cartCache.findByGuestToken(guestToken)
            val userCart = cartCache.findByUserId(userId)

            when {
                guestCart == null -> {
                    logger.info("No guest cart found for $guestToken, returning user cart")
                    getGuestCart(guestToken) // This will create a new one if needed
                }

                userCart == null -> {
                    // Guest has a cart but user doesn't - transfer ownership
                    val updatedCart = guestCart.copy(userId = userId)
                    cartCache.update(guestCart.id, updatedCart)
                    logger.info("Transferred cart ${guestCart.id} from guest $guestToken to user $userId")
                    updatedCart
                }

                else -> {
                    // Both exist - merge them
                    mergeGuestCartToUser(guestToken, userId)
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to transfer cart ownership", e)
            throw e
        }
    }
}

