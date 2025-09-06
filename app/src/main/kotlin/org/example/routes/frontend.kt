package org.example.routes

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import io.ktor.server.thymeleaf.*
import io.ktor.util.*
import org.example.domain.models.User
import org.example.plugins.AuthSession
import org.example.services.RoleService
import org.example.services.UserService
import org.koin.ktor.ext.inject

fun Route.frontendRoutes() {
    val userService by inject<UserService>()
    val roleService by inject<RoleService>()

    // Root navigation
    get("/") {
        val session = call.sessions.get<AuthSession>()

        if (session == null) {
            call.respondRedirect("/signin")
            return@get
        }

        val user = userService.getUser(session.userId)
        val roleId = user?.roleId
        val role = roleId?.let { roleService.getRole(it) }?.name

        when (role) {
            "admin" -> call.respondRedirect("/admin/dashboard")
            "customer" -> call.respondRedirect("/customer/home")
            else -> call.respondRedirect("/signin") // Handle unknown roles
        }
    }

    // Admin routes
    route("/admin/") {
        get {
            call.respondRedirect("admin/dashboard")
        }
        requireAuth(role = "admin", userService = userService, roleService = roleService) {
            get("dashboard") {
                println("Content type from headers: ${call.request.headers["Accept"]}")
                application.log.info("Content type from headers: ${call.request.headers["Accept"]}")
                call.respond(
                    ThymeleafContent(
                        "admin/pages/dashboard", mapOf(
                            "currentPage" to "dashboard"
                        )
                    )
                )
            }
        }
        requireAuth(role = "admin", userService = userService, roleService = roleService) {
            get("users") {
                val users = userService.getUsers(queryParams = emptyMap())
                val activeUsersCount = users.count { it.active }
                val inactiveUsersCount = users.count { !it.active }

                call.respond(
                    ThymeleafContent(
                        "admin/pages/users/index", mapOf(
                            "currentPage" to "users",
                            "users" to users,
                            "columns" to User.columns,
                            "rows" to User.toRows(users),
                            "activeUsersCount" to activeUsersCount,
                            "inactiveUsersCount" to inactiveUsersCount,
                            "totalUsersCount" to users.size
                        )
                    )
                )
            }
        }
        requireAuth(role = "admin", userService = userService, roleService = roleService) {
            get("products") {
                call.respond(
                    ThymeleafContent(
                        "admin/pages/products/index", mapOf(
                            "currentPage" to "products"
                        )
                    )
                )
            }
        }
        requireAuth(role = "admin", userService = userService, roleService = roleService) {
            get("orders") {
                call.respond(
                    ThymeleafContent(
                        "admin/pages/orders/index", mapOf(
                            "currentPage" to "orders"
                        )
                    )
                )
            }
        }
        requireAuth(role = "admin", userService = userService, roleService = roleService) {
            get("analytics") {
                call.respond(
                    ThymeleafContent(
                        "admin/pages/analytics", mapOf(
                            "currentPage" to "analytics"
                        )
                    )
                )
            }
        }
        requireAuth(role = "admin", userService = userService, roleService = roleService) {
            get("settings") {
                call.respond(
                    ThymeleafContent(
                        "admin/pages/settings", mapOf(
                            "currentPage" to "settings"
                        )
                    )
                )
            }
        }
        requireAuth(role = "admin", userService = userService, roleService = roleService) {
            get("help") {
                call.respond(
                    ThymeleafContent(
                        "admin/pages/help", mapOf(
                            "currentPage" to "help"
                        )
                    )
                )
            }
        }
        requireAuth(role = "admin", userService = userService, roleService = roleService) {
            get("profile") {
                call.respond(
                    ThymeleafContent(
                        "admin/pages/profile", mapOf(
                            "currentPage" to "profile"
                        )
                    )
                )
            }
        }
    }

    route("/customer/") {
        get {
            call.respondRedirect("/customer/home")
        }

        requireAuth(role = "customer", userService = userService, roleService = roleService) {
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

    route("/error/") {
        get("404") {
            call.respond(ThymeleafContent("error/404", mapOf()))
        }
        get("500") {
            call.respond(ThymeleafContent("error/500", mapOf()))
        }
    }
}
