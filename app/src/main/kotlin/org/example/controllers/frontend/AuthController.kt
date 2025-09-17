package org.example.controllers.frontend

import io.ktor.server.request.receiveParameters
import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.sessions.clear
import io.ktor.server.sessions.sessions
import io.ktor.server.sessions.set
import io.ktor.server.thymeleaf.ThymeleafContent
import io.ktor.util.toMap
import org.example.domain.models.User
import org.example.plugins.AuthSession
import org.example.services.RoleService
import org.example.services.UserService
import kotlin.text.toIntOrNull

class AuthController(
    private val userService: UserService,
    private val roleService: RoleService
) {

    fun Route.authRoutes() {
        route("/signup") {
            get {
                val offset = call.request.queryParameters["offset"]?.toIntOrNull() ?: 0
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 10
                val queryParams = call.request.queryParameters.toMap().mapValues { it.value.firstOrNull() ?: "" }
                    .filterKeys { it != "offset" && it != "limit" }

                val roles = roleService.getRoles(offset, limit, queryParams)

                val formData = User(name = "", email = "", password = "", roleId = "")
                call.respond(
                    ThymeleafContent(
                        "auth/signup",
                        mapOf("formData" to formData, "roles" to roles)
                    )
                )
            }
            post {
                val params = call.receiveParameters()
                val formData = User.formParameters(params)
                userService.createUser(formData)

                call.respondRedirect("/signin")
            }
        }
        route("signin") {
            get {
                call.respond(ThymeleafContent("auth/signin", mapOf()))
            }
            post {
                val params = call.receiveParameters()
                val email = params["email"].toString()
                val password = params["password"].toString()
                val user = userService.authenticate(email, password)

                if (user != null) {
                    call.sessions.set(AuthSession(user.id, user.email, System.currentTimeMillis()))
                }
                call.respondRedirect("/")
            }
        }

        post("/logout") {
            call.sessions.clear<AuthSession>()
            call.respondRedirect("/signin")
        }
    }
}