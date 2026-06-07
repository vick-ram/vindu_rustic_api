package org.example.services

import org.example.domain.models.Cart
import org.example.domain.models.sales.CartItem
import org.example.plugins.CartSession

class CartService(
    private val cartRepo: CartRepository,
    private val productService: ProductService
) {
    suspend fun getCart(userId: String?, sessionId: String): CartSession {
        return if (userId != null) {
            // Authenticated user - get from database
            val dbCart = cartRepo.getCartByUser(userId)
            convertToCartSession(dbCart, sessionId, userId)
        } else {
            // Guest user - create session cart
            CartSession(sessionId = sessionId)
        }
    }

    suspend fun addToCart(userId: String?, sessionId: String, productId: String, quantity: Int): CartSession {
        val product = productService.getProduct(productId) ?: throw Exception("Product not found")

        return if (userId != null) {
            // Authenticated - persist to database
            val updatedCart = cartRepo.addItem(userId, productId, quantity)
            convertToCartSession(updatedCart, sessionId, userId)
        } else {
            // Guest - store in session
            val sessionCart = CartSession(sessionId = sessionId)
            val cartItem = CartItem(
                cartId = sessionId,
                productId = productId,
                quantity = quantity,
                unitPrice = product.basePrice
            )
            sessionCart.addItem(cartItem)
            sessionCart
        }
    }

    suspend fun removeFromCart(userId: String?, sessionId: String, productId: String): CartSession {
        return if (userId != null) {
            val updatedCart = cartRepo.removeItem(userId, productId)
            convertToCartSession(updatedCart, sessionId, userId)
        } else {
            val sessionCart = CartSession(sessionId = sessionId)
            sessionCart.removeItem(productId)
            sessionCart
        }
    }

    suspend fun updateCartItem(userId: String?, sessionId: String, productId: String, quantity: Int): CartSession {
        return if (userId != null) {
            val updatedCart = cartRepo.updateItem(userId, productId, quantity)
            convertToCartSession(updatedCart, sessionId, userId)
        } else {
            val sessionCart = CartSession(sessionId = sessionId)
            sessionCart.updateQuantity(productId, quantity)
            sessionCart
        }
    }

    suspend fun mergeCarts(sessionCart: CartSession, userId: String): Cart {
        var userCart = cartRepo.getCartByUser(userId)

        // If user has no cart, create one with session items
        if (userCart == null && sessionCart.items.isNotEmpty()) {
            // Add first item to create cart
            val firstItem = sessionCart.items.first()
            userCart = cartRepo.addItem(userId, firstItem.productId, firstItem.quantity)

            // Add remaining items
            sessionCart.items.drop(1).forEach { item ->
                userCart = cartRepo.addItem(userId, item.productId, item.quantity)
            }
        } else if (userCart != null && sessionCart.items.isNotEmpty()) {
            // Merge session items into existing cart
            sessionCart.items.forEach { sessionItem ->
                val existingItem = userCart?.items?.find { it.productId == sessionItem.productId }
                userCart = if (existingItem != null) {
                    cartRepo.updateItem(userId, sessionItem.productId, existingItem.quantity + sessionItem.quantity)
                } else {
                    cartRepo.addItem(userId, sessionItem.productId, sessionItem.quantity)
                }
            }
        }

        return userCart ?: Cart(userId = userId)
    }

    private fun convertToCartSession(dbCart: Cart?, sessionId: String, userId: String): CartSession {
        return if (dbCart != null) {
            CartSession(
                sessionId = sessionId,
                userId = userId,
                items = dbCart.items.toMutableList(),
                total = dbCart.totalPrice
            )
        } else {
            CartSession(sessionId = sessionId, userId = userId)
        }
    }
}
