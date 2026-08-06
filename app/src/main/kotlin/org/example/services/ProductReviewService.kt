package org.example.services

import kotlinx.serialization.Serializable
import org.example.data.cache.ProductReviewCache
import org.example.di.Component
import org.example.di.Inject
import org.example.domain.models.catalog.ProductReview
import kotlin.math.roundToInt

@Component
class ProductReviewService @Inject constructor(
    private val productReviewCache: ProductReviewCache
) {
    suspend fun createReview(review: ProductReview): ProductReview {
        return productReviewCache.create(review)
    }

    suspend fun getReview(id: String): ProductReview? {
        return productReviewCache.read(id)
    }

    suspend fun getAllReviews(
        offset: Int = 0,
        limit: Int = 20,
        queryParams: Map<String, String>? = null
    ): List<ProductReview> {
        return productReviewCache.readAll(offset, limit, queryParams)
    }

    suspend fun updateReview(id: String, review: ProductReview): ProductReview? {
        return productReviewCache.update(id, review)
    }

    suspend fun deleteReview(id: String): Boolean {
        return productReviewCache.delete(id)
    }

    suspend fun getProductReviews(
        productId: String,
        offset: Int = 0,
        limit: Int = 20
    ): List<ProductReview> {
        return productReviewCache.findByProductId(productId, offset, limit)
    }

    suspend fun getVerifiedProductReviews(productId: String): List<ProductReview> {
        return productReviewCache.findVerifiedByProductId(productId)
    }

    suspend fun getProductAverageRating(productId: String): Double? {
        return productReviewCache.getAverageRating(productId)
    }

    suspend fun getProductRatingDistribution(productId: String): Map<Int, Int> {
        val distribution = productReviewCache.getRatingDistribution(productId)

        // Ensure all ratings 1-5 are represented, even if count is 0
        return (1..5).associateWith { rating ->
            distribution[rating] ?: 0
        }
    }

    suspend fun getUserReviews(userId: String): List<ProductReview> {
        return productReviewCache.findByUserId(userId)
    }

    suspend fun getProductReviewSummary(productId: String): ReviewSummary {
        val avgRating = getProductAverageRating(productId)
        val distribution = getProductRatingDistribution(productId)
        val totalReviews = distribution.values.sum()

        return ReviewSummary(
            productId = productId,
            averageRating = avgRating ?: 0.0,
            totalReviews = totalReviews,
            ratingDistribution = distribution,
            fiveStarPercentage = calculatePercentage(distribution[5] ?: 0, totalReviews),
            fourStarPercentage = calculatePercentage(distribution[4] ?: 0, totalReviews),
            threeStarPercentage = calculatePercentage(distribution[3] ?: 0, totalReviews),
            twoStarPercentage = calculatePercentage(distribution[2] ?: 0, totalReviews),
            oneStarPercentage = calculatePercentage(distribution[1] ?: 0, totalReviews)
        )
    }

    suspend fun getTopRatedProducts(minReviews: Int = 5, limit: Int = 10): List<ProductRating> {
        // This would typically be implemented with a more complex query
        // Here's a placeholder that would need to be implemented based on your needs
        throw UnsupportedOperationException("Top rated products feature not yet implemented")
    }

    // Cache management
    suspend fun refreshProductCache(productId: String) {
        productReviewCache.invalidateProductCaches(productId)
        // Pre-warm cache by fetching fresh data
        productReviewCache.findByProductId(productId)
        productReviewCache.getAverageRating(productId)
        productReviewCache.getRatingDistribution(productId)
    }

    suspend fun refreshUserCache(userId: String) {
        productReviewCache.invalidateUserCaches(userId)
        // Pre-warm cache
        productReviewCache.findByUserId(userId)
    }

    suspend fun clearAllReviewCaches() {
        productReviewCache.clearAllCaches()
    }

    private fun calculatePercentage(count: Int, total: Int): Double {
        return if (total > 0) (count.toDouble() / total * 100).roundToTwoDecimals() else 0.0
    }

    private fun Double.roundToTwoDecimals(): Double {
        return (this * 100.0).roundToInt() / 100.0
    }
}

@Serializable
data class ReviewSummary(
    val productId: String,
    val averageRating: Double,
    val totalReviews: Int,
    val ratingDistribution: Map<Int, Int>,
    val fiveStarPercentage: Double,
    val fourStarPercentage: Double,
    val threeStarPercentage: Double,
    val twoStarPercentage: Double,
    val oneStarPercentage: Double
)

@Serializable
data class ProductRating(
    val productId: String,
    val averageRating: Double,
    val totalReviews: Int,
    val verifiedPurchasePercentage: Double
)