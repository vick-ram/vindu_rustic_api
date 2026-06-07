package org.example.data.db.entities

import org.example.data.db.config.CustomEntity
import org.example.data.db.config.CustomEntityClass
import org.example.data.db.tables.WishlistItems
import org.example.data.db.tables.Wishlists
import org.jetbrains.exposed.v1.core.dao.id.EntityID

class WishlistEntity(id: EntityID<String>) : CustomEntity(id, Wishlists) {
    companion object : CustomEntityClass<WishlistEntity>(Wishlists)

    var userId by Wishlists.userId
    var name by Wishlists.name
    var isPublic by Wishlists.isPublic

    val items by WishlistItemEntity referrersOn WishlistItems.wishlistId
}