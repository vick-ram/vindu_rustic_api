package org.example.controllers.frontend

import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions
import io.ktor.server.thymeleaf.ThymeleafContent
import org.example.plugins.AuthSession
import org.example.routes.requireAuth
import org.example.services.RoleService
import org.example.services.UserService

class EcommerceController(
    private val userService: UserService,
    private val roleService: RoleService
) {
    fun Route.ecommerceRoutes() {
        route("/ecommerce/") {
            get {
                val session = call.sessions.get<AuthSession>()
                val user = session?.let { userService.getUser(it.userId) }
                val model: MutableMap<String, Any> = mutableMapOf()

                model["currentPage"] = "home"
                model["isLoggedIn"] = user != null
                if (user != null) {
                    model["user"] = user
                }

                call.respond(
                    ThymeleafContent(
                        "ecommerce/pages/index", model
                    )
                )
            }

            get("cart") {
                val session = call.sessions.get<AuthSession>()
                if (session == null) {
                    call.respondRedirect("/signin?redirect=/ecommerce/pages/cart")
                    return@get
                }

                //Show the car for authenticated
                call.respond(ThymeleafContent("ecommerce/pages/cart", mapOf()))
            }

            get("checkout") {
                val session = call.sessions.get<AuthSession>()
                if (session == null) {
                    call.respondRedirect("/signin?redirect=/ecommerce/pages/cart")
                    return@get
                }

                val user = userService.getUser(session.userId)
                val role = user?.roleId?.let { roleService.getRole(it) }?.name

                if (role != "customer") {
                    call.respondRedirect("/ecommerce?error=access_denied")
                    return@get
                }
                call.respond(ThymeleafContent("ecommerce/pages/checkout", mapOf()))
            }
        }
    }
}