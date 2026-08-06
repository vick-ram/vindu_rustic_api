package org.example.services

import org.example.data.cache.CartItemCache
import org.example.data.repo.CartTotal
import org.example.di.Component
import org.example.di.Inject
import org.example.domain.models.sales.CartItem

@Component
class CartItemService @Inject constructor(private val cache: CartItemCache) {
    suspend fun getItems(cartId: String): List<CartItem> = cache.findByCartId(cartId)
    suspend fun getItem(cartId: String, variantId: String, customizationDetails: Map<String, Any>? = null) =
        cache.findByCartAndVariant(cartId, variantId, customizationDetails)
    suspend fun addOrUpdateItem(cartId: String, variantId: String, quantity: Int, customizationDetails: Map<String, Any>? = null): CartItem =
        cache.addOrUpdateItem(cartId, variantId, quantity, customizationDetails)
    suspend fun updateQuantity(id: String, quantity: Int) = cache.updateQuantity(id, quantity)
    suspend fun getCartTotal(cartId: String): CartTotal? = cache.getCartTotal(cartId)
    suspend fun clearCart(cartId: String): Int = cache.clearCart(cartId)
    suspend fun isVariantInAnyCart(variantId: String): Boolean = cache.isVariantInAnyCart(variantId)
    suspend fun deleteItem(id: String): Boolean = cache.delete(id)
}
