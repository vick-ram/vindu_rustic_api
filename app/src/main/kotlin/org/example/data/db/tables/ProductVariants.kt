package org.example.data.db.tables

import kotlinx.serialization.json.Json
import org.example.data.db.config.CustomTable
import org.jetbrains.exposed.v1.json.jsonb

object ProductVariants : CustomTable("product_variants") {
    val productId = reference("product_id", Products)
    val sku = varchar("sku", 100).uniqueIndex()
    val title = varchar("title", 255).nullable()
    val price = decimal("price", 12, 2)
    val compareAtPrice = decimal("compare_at_price", 12, 2).nullable()
    val costPrice = decimal("cost_price", 12, 2).nullable()
    val currency = varchar("currency", 10).default("KES")
    val quantityInStock = integer("quantity_in_stock").default(0)
    val reservedQuantity = integer("reserved_quantity").default(0)
    val weightGrams = integer("weight_grams").nullable()
    val dimensions = jsonb<Map<String, Any>>("dimensions", Json).default(emptyMap())
    val attributes = jsonb<Map<String, Any>>("attributes", Json).default(emptyMap())
    val barcode = varchar("barcode", 255).nullable()
    val isActive = bool("is_active").default(true)
}