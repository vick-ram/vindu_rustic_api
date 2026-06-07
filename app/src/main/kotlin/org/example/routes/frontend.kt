package org.example.routes

import io.ktor.server.response.respondText
import io.ktor.server.routing.*
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import io.ktor.websocket.send
import org.example.controllers.frontend.AdminController
import org.example.controllers.frontend.AuthController
import org.example.controllers.frontend.EcommerceController
import org.example.controllers.frontend.RootController
import org.example.plugins.AuthSession
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
