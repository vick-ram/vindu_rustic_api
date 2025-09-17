package org.example.controllers.backend

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.util.toMap
import org.example.domain.models.ProductReview
import org.example.services.ProductReviewService
import org.example.utils.respondApi

class ProductReviewController(private val productReviewService: ProductReviewService) {
    fun Route.routes() {
        route("/product-review/") {
            post {
                val productReviewRequest = call.receive<ProductReview>().validate()
                val newProductReview = productReviewService.createReview(productReviewRequest)
                call.respondApi(
                    status = HttpStatusCode.Created,
                    data = newProductReview,
                    message = "Product review created successfully"
                )
            }
            put("{id}") {
                val productReviewId = call.parameters["id"] ?: ""
                val productReviewUpdateRequest = call.receive<ProductReview>()
                val updatedProduct = productReviewService.updateReview(productReviewId, productReviewUpdateRequest)
                call.respondApi(
                    status = HttpStatusCode.Accepted,
                    data = updatedProduct,
                    message = "Product review updated successfully"
                )
            }

            get("{id}") {
                val productReviewId = call.parameters["id"] ?: ""
                val productReview = productReviewService.getReview(productReviewId)
                call.respondApi(
                    data = productReview,
                    message = "Review fetched successfully"
                )
            }

            get {
                val offset = call.request.queryParameters["offset"]?.toIntOrNull() ?: 0
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 10
                val queryParams = call.request.queryParameters.toMap().mapValues { it.value.firstOrNull() ?: "" }
                    .filterKeys { it != "offset" && it != "limit" }

                val productReviews = productReviewService.getAllReviews(offset, limit, queryParams)
                call.respondApi(
                    data = productReviews,
                    message = "Reviews fetched successfully"
                )
            }

            get("product/{id}") {
                val productId = call.parameters["id"] ?: ""
                val offset = call.request.queryParameters["offset"]?.toIntOrNull() ?: 0
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 10
                val queryParams = call.request.queryParameters.toMap().mapValues { it.value.firstOrNull() ?: "" }
                    .filterKeys { it != "offset" && it != "limit" }

                val productReviews = productReviewService.getProductReviews(productId,offset, limit, queryParams)
                call.respondApi(
                    data = productReviews,
                    message = "Product Reviews fetched successfully"
                )
            }

            get("user/{id}") {
                val userId = call.parameters["id"] ?: ""
                val offset = call.request.queryParameters["offset"]?.toIntOrNull() ?: 0
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 10
                val queryParams = call.request.queryParameters.toMap().mapValues { it.value.firstOrNull() ?: "" }
                    .filterKeys { it != "offset" && it != "limit" }

                val userReviews = productReviewService.getUsersProductReviews(userId,offset, limit, queryParams)
                call.respondApi(
                    data = userReviews,
                    message = "Product Reviews fetched successfully"
                )
            }

            get("approved") {
                val offset = call.request.queryParameters["offset"]?.toIntOrNull() ?: 0
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 10
                val queryParams = call.request.queryParameters.toMap().mapValues { it.value.firstOrNull() ?: "" }
                    .filterKeys { it != "offset" && it != "limit" }

                val approvedReviews = productReviewService.getApprovedReviews(offset, limit, queryParams)
                call.respondApi(
                    data = approvedReviews,
                    message = "Product Reviews fetched successfully"
                )
            }

            delete("{id}") {
                val productReviewId = call.parameters["id"] ?: ""
                val deleted = productReviewService.deleteReview(productReviewId)
                if (deleted) {
                    call.respondApi<Unit>(
                        status = HttpStatusCode.NoContent,
                        message = "Review deleted successfully"
                    )
                }
            }
        }
    }
}