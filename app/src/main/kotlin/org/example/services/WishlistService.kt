package org.example.services

import org.example.data.cache.WishlistCache
import org.example.data.repo.WishlistWithItemCount
import org.example.di.Component
import org.example.di.Inject
import org.example.domain.models.sales.Wishlist

@Component
class WishlistService @Inject constructor(private val cache: WishlistCache) {
    suspend fun createWishlist(wishlist: Wishlist): Wishlist = cache.create(wishlist)
    suspend fun getWishlist(id: String): Wishlist? = cache.read(id)
    suspend fun updateWishlist(id: String, wishlist: Wishlist): Wishlist? = cache.update(id, wishlist)
    suspend fun deleteWishlist(id: String): Boolean = cache.delete(id)
    suspend fun getWishlistsByUser(userId: String): List<Wishlist> = cache.findByUserId(userId)
    suspend fun getDefaultWishlist(userId: String): Wishlist? = cache.findDefaultByUserId(userId)
    suspend fun getOrCreateDefaultWishlist(userId: String): Wishlist = cache.getOrCreateDefault(userId)
    suspend fun getPublicWishlists(offset: Int = 0, limit: Int = 20): List<Wishlist> = cache.findPublicWishlists(offset, limit)
    suspend fun getWishlistWithItemCount(wishlistId: String): WishlistWithItemCount? = cache.getWishlistWithItemCount(wishlistId)
}
