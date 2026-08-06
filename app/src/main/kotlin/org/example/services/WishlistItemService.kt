package org.example.services

import org.example.data.cache.WishlistItemCache
import org.example.data.repo.WishlistItemWithProduct
import org.example.di.Component
import org.example.di.Inject
import org.example.domain.models.sales.WishlistItem

@Component
class WishlistItemService @Inject constructor(private val cache: WishlistItemCache) {
    suspend fun getWishlistItem(id: String): WishlistItem? = cache.read(id)
    suspend fun getWishlistItems(wishlistId: String): List<WishlistItem> = cache.findByWishlistId(wishlistId)
    suspend fun getWishlistItemsWithProductDetails(wishlistId: String): List<WishlistItemWithProduct> = cache.findByWishlistIdWithProductDetails(wishlistId)
    suspend fun isProductInWishlist(wishlistId: String, productId: String, variantId: String? = null): Boolean = cache.isProductInWishlist(wishlistId, productId, variantId)
    suspend fun addItem(wishlistId: String, productId: String, variantId: String? = null, notes: String? = null): WishlistItem? = cache.addItem(wishlistId, productId, variantId, notes)
    suspend fun removeItem(wishlistId: String, productId: String, variantId: String? = null): Boolean = cache.removeItem(wishlistId, productId, variantId)
    suspend fun moveToCart(wishlistItemId: String, cartId: String): Boolean = cache.moveToCart(wishlistItemId, cartId)
}
