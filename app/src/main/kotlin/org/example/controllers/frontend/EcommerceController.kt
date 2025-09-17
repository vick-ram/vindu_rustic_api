package org.example.controllers.frontend

import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.thymeleaf.ThymeleafContent
import org.example.routes.requireAuth
import org.example.services.RoleService
import org.example.services.UserService

class EcommerceController(
    private val userService: UserService,
    private val roleService: RoleService
) {

    fun Route.ecommerceRoutes() {
        route("/customer/") {
        requireAuth("customer", userService, roleService) {
                // Redirect
                get {
                    call.respondRedirect("/customer/home")
                }

                get("help") {
                    call.respond(
                        ThymeleafContent(
                            "customer/pages/home", mapOf(
                                "currentPage" to "home"
                            )
                        )
                    )
                }
            }
        }
    }
}