package org.example.routes

import io.ktor.server.routing.Route
import org.example.controllers.CartController
import org.example.controllers.OrderController
import org.example.controllers.RoleController
import org.example.controllers.UserController
import org.koin.ktor.ext.inject

fun Route.backendRoutes(issuer: String, audience: String, secret: String) {
    val userController by inject<UserController>()
    val roleController by inject<RoleController>()
    val orderController by inject<OrderController>()
    val cartController by inject<CartController>()

    with(userController) {
        routes(issuer, audience, secret)
    }

    with (roleController) {
        routes()
    }

    with(orderController) {
        orderRoutes()
    }
    
    with(cartController) {
        cartRoutes()
    }


}