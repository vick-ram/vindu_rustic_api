package org.example.controllers.backend

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.routing.RoutingHandler
import io.ktor.util.toMap
import org.example.config.AuthType
import org.example.config.DynamicRouteConfig
import org.example.config.DynamicRouteFactory
import org.example.config.HttpMethodType
import org.example.config.RouteGroupConfig
import org.example.domain.models.catalog.ProductReview
import org.example.services.ProductReviewService
import org.example.utils.respondApi

class ProductReviewController(private val productReviewService: ProductReviewService) {
    fun registerReviewRoutes(factory: DynamicRouteFactory) {
        val createReviewHandler: RoutingHandler = {
            val productReviewRequest = call.receive<ProductReview>().validate()
            val newProductReview = productReviewService.createReview(productReviewRequest)
            call.respondApi(
                status = HttpStatusCode.Created,
                data = newProductReview,
                message = "Product review created successfully"
            )
        }
        val updateReviewHandler: RoutingHandler = {
            val productReviewId = call.parameters["id"] ?: ""
            val productReviewUpdateRequest = call.receive<ProductReview>()
            val updatedProduct = productReviewService.updateReview(productReviewId, productReviewUpdateRequest)
            call.respondApi(
                status = HttpStatusCode.Accepted,
                data = updatedProduct,
                message = "Product review updated successfully"
            )
        }

        val getReviewHandler: RoutingHandler = {
            val productReviewId = call.parameters["id"] ?: ""
            val productReview = productReviewService.getReview(productReviewId)
            call.respondApi(
                data = productReview,
                message = "Review fetched successfully"
            )
        }

        val getReviewsHandler: RoutingHandler = {
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

        val getProductReviewsHandler: RoutingHandler = {
            val productId = call.parameters["id"] ?: ""
            val offset = call.request.queryParameters["offset"]?.toIntOrNull() ?: 0
            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 10
            val queryParams = call.request.queryParameters.toMap().mapValues { it.value.firstOrNull() ?: "" }
                .filterKeys { it != "offset" && it != "limit" }

            val productReviews = productReviewService.getProductReviews(productId, offset, limit, queryParams)
            call.respondApi(
                data = productReviews,
                message = "Product Reviews fetched successfully"
            )
        }

        val getUserReviewsHandler: RoutingHandler = {
            val userId = call.parameters["id"] ?: ""
            val offset = call.request.queryParameters["offset"]?.toIntOrNull() ?: 0
            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 10
            val queryParams = call.request.queryParameters.toMap().mapValues { it.value.firstOrNull() ?: "" }
                .filterKeys { it != "offset" && it != "limit" }

            val userReviews = productReviewService.getUsersProductReviews(userId, offset, limit, queryParams)
            call.respondApi(
                data = userReviews,
                message = "Product Reviews fetched successfully"
            )
        }

        val getApprovedReviewsHandler: RoutingHandler = {
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

        val deleteReviewHandler: RoutingHandler = {
            val productReviewId = call.parameters["id"] ?: ""
            val deleted = productReviewService.deleteReview(productReviewId)
            if (deleted) {
                call.respondApi<Unit>(
                    status = HttpStatusCode.NoContent,
                    message = "Review deleted successfully"
                )
            }
        }

        val group = RouteGroupConfig(
            prefix = "/api/product-reviews",
            version = "1.0.0"
        )

        val routes = listOf(
            DynamicRouteConfig(
                path = "",
                methods = setOf(HttpMethodType.POST),
                handler = createReviewHandler,
                name = "CreateReview",
                requiresAuth = true,
                authType = AuthType.JWT
            ),
            DynamicRouteConfig(
                path = "/{id}",
                methods = setOf(HttpMethodType.PUT),
                handler = updateReviewHandler,
                name = "UpdateReview",
                requiresAuth = true,
                authType = AuthType.JWT,
                metadata = mapOf("tag" to "review")
            ),
            DynamicRouteConfig(
                path = "/{id}",
                methods = setOf(HttpMethodType.GET),
                handler = getReviewHandler,
                name = "GetReview",
                requiresAuth = true,
                authType = AuthType.JWT,
                metadata = mapOf("tag" to "review")
            ),
            DynamicRouteConfig(
                path = "",
                methods = setOf(HttpMethodType.GET),
                handler = getReviewsHandler,
                name = "GetReviews",
                requiresAuth = true,
                authType = AuthType.JWT,
                metadata = mapOf("tag" to "review")
            ),
            DynamicRouteConfig(
                path = "/product/{id}",
                methods = setOf(HttpMethodType.GET),
                handler = getProductReviewsHandler,
                name = "GetProductReviews",
                requiresAuth = true,
                authType = AuthType.JWT,
                metadata = mapOf("tag" to "review")
            ),
            DynamicRouteConfig(
                path = "/user/{id}",
                methods = setOf(HttpMethodType.GET),
                handler = getUserReviewsHandler,
                name = "GetUserReviews",
                requiresAuth = true,
                authType = AuthType.JWT,
                metadata = mapOf("tag" to "review")
            ),
            DynamicRouteConfig(
                path = "/approved",
                methods = setOf(HttpMethodType.GET),
                handler = getApprovedReviewsHandler,
                name = "GetApprovedReviews",
                requiresAuth = true,
                authType = AuthType.JWT,
                metadata = mapOf("tag" to "review")
            ),
            DynamicRouteConfig(
                path = "/{id}",
                methods = setOf(HttpMethodType.GET),
                handler = deleteReviewHandler,
                name = "DeleteReview",
                requiresAuth = true,
                authType = AuthType.JWT,
                metadata = mapOf("tag" to "review")
            ),
        )
        factory.registerGroup("ReviewsGroup", group, routes)
    }
}