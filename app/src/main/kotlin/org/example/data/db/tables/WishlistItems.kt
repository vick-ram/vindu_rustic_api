package org.example.data.db.tables

import org.example.data.db.config.CustomTable

object WishlistItems : CustomTable("wishlist_items") {
    val wishlistId = reference("wishlist_id", Wishlists)
    val productId = reference("product_id", Products)
    val variantId = reference("variant_id", ProductVariants).nullable()
    val notes = text("notes").nullable()

    init {
        uniqueIndex(wishlistId, productId)
    }
}