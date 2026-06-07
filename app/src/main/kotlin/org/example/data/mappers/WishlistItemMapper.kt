package org.example.data.mappers

import org.example.data.db.entities.WishlistItemEntity
import org.example.data.db.tables.ProductVariants
import org.example.data.db.tables.Products
import org.example.data.db.tables.Wishlists
import org.example.domain.models.sales.WishlistItem
import org.example.domain.repo.EntityMapper
import org.jetbrains.exposed.v1.core.dao.id.EntityID

object WishlistItemMapper : EntityMapper<WishlistItemEntity, WishlistItem, String> {
    override fun toModel(entity: WishlistItemEntity): WishlistItem {
        return WishlistItem(
            id = entity.id.value,
            wishlistId = entity.wishlistId.value,
            productId = entity.productId.value,
            variantId = entity.variantId?.value,
            notes = entity.notes,
            createdAt = entity.createdAt,
        )
    }

    override fun toEntity(model: WishlistItem, entity: WishlistItemEntity): WishlistItemEntity {
        entity.wishlistId = EntityID(model.wishlistId, Wishlists)
        entity.productId = EntityID(model.productId, Products)
        entity.variantId = model.variantId?.let { EntityID(it, ProductVariants) }
        entity.notes = model.notes
        return entity
    }
}