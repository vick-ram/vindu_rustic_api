package org.example.controllers.frontend

import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions
import org.example.plugins.AuthSession
import org.example.services.RoleService
import org.example.services.UserService

class RootController(private val userService: UserService, private val roleService: RoleService) {
    fun Route.routes() {
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
    }
}