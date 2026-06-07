package org.example.data.db.entities

import org.example.data.db.config.CustomEntity
import org.example.data.db.config.CustomEntityClass
import org.example.data.db.tables.CartItems
import org.example.data.db.tables.ShoppingCarts
import org.jetbrains.exposed.v1.core.dao.id.EntityID

class ShoppingCartEntity(id: EntityID<String>) : CustomEntity(id, ShoppingCarts) {
    companion object : CustomEntityClass<ShoppingCartEntity>(ShoppingCarts)

    var userId by ShoppingCarts.userId
    var guestToken by ShoppingCarts.guestToken

    val items by CartItemEntity referrersOn CartItems.cartId
}