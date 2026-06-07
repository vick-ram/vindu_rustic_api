package org.example.data.mappers

import org.example.data.db.entities.ShoppingCartEntity
import org.example.data.db.tables.Users
import org.example.domain.models.sales.ShoppingCart
import org.example.domain.repo.EntityMapper
import org.jetbrains.exposed.v1.core.dao.id.EntityID

object ShoppingCartMapper : EntityMapper<ShoppingCartEntity, ShoppingCart, String> {
    override fun toModel(entity: ShoppingCartEntity): ShoppingCart {
        return ShoppingCart(
            id = entity.id.value,
            userId = entity.userId?.value,
            guestToken = entity.guestToken,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }

    override fun toEntity(model: ShoppingCart, entity: ShoppingCartEntity): ShoppingCartEntity {
        entity.userId = model.userId?.let { EntityID(it, Users) }
        entity.guestToken = model.guestToken
        return entity
    }
}