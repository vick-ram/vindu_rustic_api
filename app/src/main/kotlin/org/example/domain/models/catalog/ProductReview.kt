package org.example.domain.models.catalog

import io.ktor.http.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.example.data.db.config.Ulid
import org.example.domain.validations.Between
import org.example.utils.OffsetDateTimeSerializer
import java.time.OffsetDateTime

@Serializable
data class ProductReview(
    val id: String = Ulid.generate(),

    @SerialName(value = "product_id")
    val productId: String,

    @SerialName(value = "user_id")
    val userId: String,

    @SerialName(value = "order_item_id")
    val orderItemId: String,

    @field:Between(min = 1, max = 5)
    val rating: Int, // 1-5
    val title: String,
    val review: String,

    @SerialName(value = "is_verified_purchase")
    val isVerifiedPurchase: Boolean = false,

    @Serializable(with = OffsetDateTimeSerializer::class)
    @SerialName(value = "created_at")
    val createdAt: OffsetDateTime = OffsetDateTime.now()
) {

    companion object {
        fun formParameters(parameters: Parameters): ProductReview {
            val productId = parameters["productId"].toString()
            val userId = parameters["userId"].toString()
            val orderItemId = parameters["orderItemId"].toString()
            val rating = parameters["rating"].toString().toInt()
            val title = parameters["title"].toString()
            val review = parameters["review"].toString()

            return ProductReview(
                productId = productId,
                userId = userId,
                orderItemId = orderItemId,
                rating = rating,
                title = title,
                review = review
            )
        }

        val columns: List<Map<String, Any>> =listOf(
            mapOf("key" to "id", "label" to "ID", "sortable" to true),
            mapOf("key" to "product", "label" to "Product", "sortable" to true),
            mapOf("key" to "user", "label" to "User", "sortable" to true),
            mapOf("key" to "rating", "label" to "Rating", "sortable" to true),
            mapOf("key" to "title", "label" to "Title", "sortable" to true),
            mapOf("key" to "review", "label" to "Review", "sortable" to true),
            mapOf("key" to "isVerifiedPurchase", "label" to "Is Verified Purchase", "sortable" to true),
            mapOf("key" to "createdAt", "label" to "Created At", "sortable" to true)
        )

        fun toRows(productReviews: List<ProductReview>): List<Map<String, Any?>> = productReviews
            .map { productReview ->
                mapOf(
                    "id" to productReview.id,
                    "product" to productReview.productId,
                    "user" to productReview.userId,
                    "rating" to productReview.rating,
                    "title" to productReview.title,
                    "review" to productReview.review,
                    "isVerifiedPurchase" to productReview.isVerifiedPurchase,
                    "createdAt" to productReview.createdAt
                )
            }
    }
}
