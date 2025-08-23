package org.example.services

import org.example.domain.models.Cart
import org.example.domain.repo.CartRepository

class CartService(private val cartRepo: CartRepository) {
    suspend fun getCart(userId: String): Cart? = cartRepo.getCartByUser(userId)

    suspend fun addToCart(userId: String, productId: String, quantity: Int): Cart {
        return cartRepo.addItem(userId, productId, quantity)
    }

    suspend fun removeFromCart(userId: String, productId: String): Cart {
        return cartRepo.removeItem(userId, productId)
    }

    suspend fun updateCartItem(userId: String, productId: String, quantity: Int): Cart {
        return cartRepo.updateItem(userId, productId, quantity)
    }

    suspend fun clearCart(userId: String) {
        cartRepo.clearCart(userId)
    }
}