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
import kotlinx.datetime.LocalDateTime
import org.example.domain.models.User
import org.example.plugins.AuthSession
import org.example.services.RoleService
import org.example.services.UserService
import org.example.utils.now
import kotlin.text.toIntOrNull

class AuthController(
    private val userService: UserService,
    private val roleService: RoleService
) {

    fun Route.authRoutes() {
        route("/signup") {
            get {
                val roles = roleService.getRoles(0, 10, emptyMap())

                val formData = User(
                    name = "",
                    email = "",
                    password = "",
                    roleId = "",
                    createdAt = LocalDateTime.now(),
                    updatedAt = LocalDateTime.now()
                )
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
        route("/signin") {
            get {
                val redirectUrl = call.request.queryParameters["redirect"] ?: "/"
                call.respond(ThymeleafContent("auth/signin", mapOf("redirectUrl" to redirectUrl)))
            }
            post {
                val params = call.receiveParameters()
                val email = params["email"].toString()
                val password = params["password"].toString()
                val redirectUrl = params["redirectUrl"] ?: "/"
                val user = userService.authenticate(email, password)

                if (user != null) {
                    call.sessions.set(AuthSession(user.id, user.email, System.currentTimeMillis()))

                    // Determine where to redirect based on role
                    val role = user.roleId.let { roleService.getRole(it) }?.name
                    val targetUrl = when {
                        redirectUrl.isNotBlank() && redirectUrl != "/" ->redirectUrl
                        role == "admin" -> "/admin/dashboard"
                        else -> "/ecommerce"
                    }
                    call.respondRedirect(targetUrl)
                } else {
                    call.respondRedirect("/signin?error=invalid_credentials")
                }
            }
        }

        get("/forgot-password") {
            call.respond(ThymeleafContent("auth/forgot-password", mapOf()))
        }

        post("/logout") {
            call.sessions.clear<AuthSession>()
            call.respondRedirect("/ecommerce")
        }
    }
}