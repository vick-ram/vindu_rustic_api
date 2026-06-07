package org.example.data.db.entities

import org.example.data.db.config.CustomEntity
import org.example.data.db.config.CustomEntityClass
import org.example.data.db.tables.ProductReviews
import org.jetbrains.exposed.v1.core.dao.id.EntityID

class ProductReviewEntity(id: EntityID<String>): CustomEntity(id, ProductReviews) {
    companion object : CustomEntityClass<ProductReviewEntity>(ProductReviews)

    var productId by ProductReviews.productId
    var userId by ProductReviews.userId
    var orderItemId by ProductReviews.orderItemId
    var rating by ProductReviews.rating
    var title by ProductReviews.title
    var review by ProductReviews.review
    var isVerifiedPurchase by ProductReviews.isVerifiedPurchase
}