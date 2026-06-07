package org.example.data.db.entities

import org.example.data.db.config.CustomEntity
import org.example.data.db.config.CustomEntityClass
import org.example.data.db.tables.WishlistItems
import org.jetbrains.exposed.v1.core.dao.id.EntityID

class WishlistItemEntity(id: EntityID<String>) : CustomEntity(id, WishlistItems) {
    companion object : CustomEntityClass<WishlistItemEntity>(WishlistItems)

    var wishlistId by WishlistItems.wishlistId
    var productId by WishlistItems.productId
    var variantId by WishlistItems.variantId
    var notes by WishlistItems.notes
}