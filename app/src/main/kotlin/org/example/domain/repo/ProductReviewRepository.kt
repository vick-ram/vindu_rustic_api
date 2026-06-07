package org.example.domain.repo

import org.example.domain.models.catalog.ProductReview

interface ProductReviewRepository: CrudRepository<ProductReview, String> {
    suspend fun fetchProductReviews(productId: String, offset: Int, limit: Int, queryParams: Map<String, String>?): List<ProductReview>
    suspend fun fetchUsersProductReviews(userId: String, offset: Int, limit: Int, queryParams: Map<String, String>?): List<ProductReview>
    suspend fun fetchApprovedReviews(offset: Int, limit: Int, queryParams: Map<String, String>?): List<ProductReview>
}