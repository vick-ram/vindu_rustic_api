package org.example.domain.repo

import org.example.domain.models.sales.ShoppingCart
import java.util.UUID

interface ShoppingCartRepository : CrudRepository<ShoppingCart, String> {
    suspend fun findByUserId(userId: String): ShoppingCart?
    suspend fun findByGuestToken(token: UUID): ShoppingCart?
    suspend fun getOrCreateCart(userId: String?, guestToken: UUID?): ShoppingCart
    suspend fun clearCart(cartId: String): Boolean
}