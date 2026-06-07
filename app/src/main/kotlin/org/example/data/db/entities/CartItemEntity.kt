package org.example.data.db.entities

import org.example.data.db.config.CustomEntity
import org.example.data.db.config.CustomEntityClass
import org.example.data.db.tables.CartItems
import org.jetbrains.exposed.v1.core.dao.id.EntityID

class CartItemEntity(id: EntityID<String>) : CustomEntity(id, CartItems) {
    companion object : CustomEntityClass<CartItemEntity>(CartItems)

    var cartId by CartItems.cartId
    var variantId by CartItems.variantId
    var quantity by CartItems.quantity
    var customizationDetails by CartItems.customizationDetails
}