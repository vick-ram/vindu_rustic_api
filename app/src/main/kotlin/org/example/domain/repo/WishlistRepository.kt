package org.example.domain.repo

import org.example.domain.models.sales.Wishlist
import org.example.domain.models.sales.WishlistItem

interface WishlistRepository : CrudRepository<Wishlist, String> {
    suspend fun findByUserId(userId: String): List<Wishlist>
    suspend fun addItem(wishlistId: String, productId: String, variantId: String?): WishlistItem
    suspend fun removeItem(wishlistId: String, productId: String): Boolean
}