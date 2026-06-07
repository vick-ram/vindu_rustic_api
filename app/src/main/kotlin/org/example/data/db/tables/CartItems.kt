package org.example.data.db.tables

import org.example.data.db.config.CustomTable
import org.example.data.db.config.gsonJsonb

object CartItems : CustomTable("cart_items") {
    val cartId = reference("cart_id", ShoppingCarts)
    val variantId = reference("variant_id", ProductVariants)
    val quantity = integer("quantity").check { it greater 0 }
    val customizationDetails = gsonJsonb<Map<String, Any>>("customization_details").nullable()
}