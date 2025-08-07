package org.example.domain.repo

import org.example.domain.models.Cart

interface CartRepository {
    suspend fun getCartByUser(userId: String): Cart?
    suspend fun addItem(userId: String, productId: String, quantity: Int): Cart
    suspend fun removeItem(userId: String, productId: String): Cart
    suspend fun updateItem(userId: String, productId: String, quantity: Int): Cart
    suspend fun clearCart(userId: String)
}