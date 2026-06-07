package org.example.data.db.tables

import org.example.data.db.config.CustomTable

object ProductMedia : CustomTable("product_media") {
    val productId = reference("product_id", Products)
    val variantId = reference("variant_id", ProductVariants).nullable()
    val mediaType = varchar("media_type", 50)
    val mediaUrl = text("media_url")
    val altText = text("alt_text").nullable()
    val sortOrder = integer("sort_order").default(0)
}