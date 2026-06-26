package org.example.data.db.tables

import kotlinx.serialization.json.Json
import org.example.data.db.config.CustomTable
import org.jetbrains.exposed.v1.json.jsonb

object CartItems : CustomTable("cart_items") {
    val cartId = reference("cart_id", ShoppingCarts)
    val variantId = reference("variant_id", ProductVariants)
    val quantity = integer("quantity").check { it greater 0 }
    val customizationDetails = jsonb<Map<String, Any>>("customization_details", Json).nullable()
}