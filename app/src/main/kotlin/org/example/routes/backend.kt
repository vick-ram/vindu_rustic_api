package org.example.routes

import io.ktor.server.routing.Route
import org.example.controllers.CartController
import org.example.controllers.CategoryController
import org.example.controllers.OrderController
import org.example.controllers.ProductController
import org.example.controllers.ProductReviewController
import org.example.controllers.RoleController
import org.example.controllers.UserController
import org.koin.ktor.ext.inject

fun Route.backendRoutes(issuer: String, audience: String, secret: String) {
    val userController by inject<UserController>()
    val roleController by inject<RoleController>()
    val orderController by inject<OrderController>()
    val cartController by inject<CartController>()
    val productReviewController by inject<ProductReviewController>()
    val categoryController by inject<CategoryController>()
    val productController by inject<ProductController>()

    with(userController) {
        routes(issuer, audience, secret)
    }

    with (roleController) {
        routes()
    }

    with(categoryController) {
        categoryRoutes()
    }

    with(productController) {
        productRoutes()
    }

    with(productReviewController) {
        routes()
    }

    with(cartController) {
        cartRoutes()
    }

    with(orderController) {
        orderRoutes()
    }

}