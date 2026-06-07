package org.example.data.db.tables

import org.example.data.db.config.CustomTable

object ProductReviews: CustomTable("product_reviews") {
    val productId = reference("product_id", Products)
    val userId = reference("user_id", Users)
    val orderItemId = reference("order_item_id", OrderItems)
    val rating = integer("rating")
    val title = varchar("title", 255)
    val review = text("review")
    val isVerifiedPurchase = bool("is_verified_purchase").default(false)
}