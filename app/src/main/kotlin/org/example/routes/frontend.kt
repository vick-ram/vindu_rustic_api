package org.example.routes

import io.ktor.server.routing.*
import org.example.controllers.frontend.AdminController
import org.example.controllers.frontend.AuthController
import org.example.controllers.frontend.EcommerceController
import org.example.controllers.frontend.RootController
import org.koin.ktor.ext.inject

fun Route.frontendRoutes() {
    val rootController by inject<RootController>()
    val authController by inject<AuthController>()
    val adminController by inject<AdminController>()
    val ecommerceController by inject<EcommerceController>()

    with(rootController) {
        routes()
    }

    with(authController) {
        authRoutes()
    }

    with(adminController) {
        adminRoutes()
    }

    with(ecommerceController) {
        ecommerceRoutes()
    }
}
