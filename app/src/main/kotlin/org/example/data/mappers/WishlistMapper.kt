package org.example.data.mappers

import org.example.data.db.entities.WishlistEntity
import org.example.data.db.tables.Users
import org.example.domain.models.sales.Wishlist
import org.example.domain.repo.EntityMapper
import org.jetbrains.exposed.v1.core.dao.id.EntityID

object WishlistMapper : EntityMapper<WishlistEntity, Wishlist, String> {
    override fun toModel(entity: WishlistEntity): Wishlist {
        return Wishlist(
            id = entity.id.value,
            userId = entity.userId.value,
            name = entity.name,
            isPublic = entity.isPublic,
            createdAt = entity.createdAt,
        )
    }

    override fun toEntity(model: Wishlist, entity: WishlistEntity): WishlistEntity {
        entity.userId = EntityID(model.userId, Users)
        entity.name = model.name
        entity.isPublic = model.isPublic
        return entity
    }
}