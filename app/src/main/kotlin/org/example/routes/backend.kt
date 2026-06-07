package org.example.routes

import io.ktor.server.routing.Route
import org.example.controllers.backend.CartController
import org.example.controllers.backend.CategoryController
import org.example.controllers.backend.OrderController
import org.example.controllers.backend.ProductController
import org.example.controllers.backend.ProductReviewController
import org.example.controllers.backend.RoleController
import org.example.controllers.backend.UserController
import org.example.config.DynamicRouteFactory
import org.koin.ktor.ext.inject

fun Route.backendRoutes(factory: DynamicRouteFactory, issuer: String, audience: String, secret: String) {
    val userController by inject<UserController>()
    val roleController by inject<RoleController>()
    val orderController by inject<OrderController>()
    val cartController by inject<CartController>()
    val productReviewController by inject<ProductReviewController>()
    val categoryController by inject<CategoryController>()
    val productController by inject<ProductController>()

    with(userController) { registerUserRoutes(issuer, audience, secret, factory) }
    with(roleController) { routes() }
    with(categoryController) { categoryRoutes() }
    with(productController) { registerProductRoutes(factory) }
    with(productReviewController) { registerReviewRoutes(factory) }
    with(cartController) { cartRoutes() }
    with(orderController) { orderRoutes() }
}