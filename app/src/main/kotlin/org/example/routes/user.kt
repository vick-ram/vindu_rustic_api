package org.example.routes

import io.ktor.server.routing.Route
import org.example.controllers.UserController
import org.koin.ktor.ext.inject

fun Route.userRoutes(issuer: String, audience: String, secret: String) {
    val userController by inject<UserController>()
    with(userController) {
        routes(issuer, audience, secret)
    }
}