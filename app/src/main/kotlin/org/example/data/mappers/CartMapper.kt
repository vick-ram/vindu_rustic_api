package org.example.data.mappers

import org.example.data.db.entities.CartEntity
import org.example.data.db.entities.CartItemEntity
import org.example.domain.models.Cart
import org.example.domain.models.CartItem
import org.example.domain.repo.EntityMapper

object CartMapper: EntityMapper<CartEntity, Cart, String> {
    override fun toModel(entity: CartEntity): Cart {
        return Cart(
            id = entity.id.value,
            user = UserMapper.toModel(entity.user),
            items = entity.items.map { CartItemMapper.toModel(it) },
            totalQuantity = entity.totalQuantity,
            totalPrice = entity.totalPrice,
            discount = entity.discount?.let { DiscountMapper.toModel(it) },
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }

    override fun toEntity(
        model: Cart,
        entity: CartEntity
    ): CartEntity {
        entity.user = UserMapper.toEntity(model.user)
        entity.totalQuantity = model.totalQuantity
        entity.totalPrice = model.totalPrice
        entity.discount = model.discount?.let { DiscountMapper.toEntity(it, entity.discount) }

        return entity
    }
}

object CartItemMapper: EntityMapper<CartItemEntity, CartItem, String> {
    override fun toModel(entity: CartItemEntity): CartItem {
        return CartItem(
            id = entity.id.value,
            cartId = entity.cart.id.value,
            product = ProductMapper.toModel(entity.product),
            quantity = entity.quantity,
            unitPrice = entity.unitPrice,
            totalPrice = entity.totalPrice,
            addedAt = entity.createdAt
        )
    }

    override fun toEntity(
        model: CartItem,
        entity: CartItemEntity
    ): CartItemEntity {
        entity.cart = CartEntity.new(model.cartId)
        entity.product = ProductMapper.toEntity(model.product, entity.product)
        entity.quantity = model.quantity
        entity.unitPrice = model.unitPrice
        entity.totalPrice = model.totalPrice
        entity.createdAt = model.addedAt

        return entity
    }
}