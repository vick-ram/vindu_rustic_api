package org.example.services

import org.example.domain.models.catalog.ProductReview
import org.example.domain.repo.ProductReviewRepository

class ProductReviewService(private val productReviewRepository: ProductReviewRepository) {
    suspend fun createReview(productReview: ProductReview): ProductReview? {
        return productReviewRepository.create(productReview)
    }

    suspend fun updateReview(reviewId: String, productReview: ProductReview): ProductReview? {
        return productReviewRepository.update(reviewId, productReview)
    }

    suspend fun getReview(productReviewId: String): ProductReview? {
        return productReviewRepository.read(productReviewId)
    }

    suspend fun getAllReviews(
        offset: Int,
        limit: Int,
        queryParams: Map<String, String>?
    ): List<ProductReview> {
        return productReviewRepository.readAll(offset, limit, queryParams)
    }

    suspend fun getProductReviews(
        productId: String,
        offset: Int,
        limit: Int,
        queryParams: Map<String, String>?
    ): List<ProductReview> {
        return productReviewRepository.fetchProductReviews(productId, offset, limit, queryParams)
    }

    suspend fun getUsersProductReviews(
        userId: String,
        offset: Int,
        limit: Int,
        queryParams: Map<String, String>?
    ): List<ProductReview> {
        return productReviewRepository.fetchUsersProductReviews(userId, offset, limit, queryParams)
    }

    suspend fun getApprovedReviews(
        offset: Int,
        limit: Int,
        queryParams: Map<String, String>?
    ): List<ProductReview> {
        return productReviewRepository.fetchApprovedReviews(offset, limit, queryParams)
    }

    suspend fun deleteReview(reviewId: String): Boolean {
        return productReviewRepository.delete(reviewId)
    }

}