package org.example.services

import org.example.domain.models.sales.ShoppingCart
import org.example.domain.repo.ShoppingCartRepository

class ShoppingCartService(private val cartRepo: ShoppingCartRepository) {

    suspend fun createCart(shoppingCart: ShoppingCart): ShoppingCart {
        return cartRepo.create(shoppingCart)
    }

    suspend fun updateCart(id: String, shoppingCart: ShoppingCart): ShoppingCart? {
        return cartRepo.update(id, shoppingCart)
    }

    suspend fun deleteCart(id: String): Boolean {
        return cartRepo.delete(id)
    }

    suspend fun getCart(id: String): ShoppingCart? {
        return cartRepo.read(id)
    }

    suspend fun getCart(userId: String?, guestToken: java.util.UUID?) =
        cartRepo.getOrCreateCart(userId, guestToken)

    suspend fun clearCart(cartId: String) =
        cartRepo.clearCart(cartId)

    suspend fun findByUserId(userId: String) =
        cartRepo.findByUserId(userId)

    suspend fun findByGuestToken(token: java.util.UUID) =
        cartRepo.findByGuestToken(token)

}