package org.example.controllers.frontend

import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.sessions.clear
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions
import org.example.di.Component
import org.example.di.Inject
import org.example.plugins.AuthSession
import org.example.services.RoleService
import org.example.services.UserService

@Component
class RootController @Inject constructor(private val userService: UserService, private val roleService: RoleService) {
    fun Route.routes() {
        get("/") {
            val session = call.sessions.get<AuthSession>()

            if (session == null) {
                call.respondRedirect("/ecommerce")
                return@get
            }

//            val user = userService.getUser(session.userId)
//            val roleId = user?.roleId
//            val role = roleId?.let { roleService.getRole(it) }?.name
//
//            when (role) {
//                "admin" -> call.respondRedirect("/admin/dashboard")
//                "customer" -> call.respondRedirect("/ecommerce/home")
//                else -> {
//                    call.sessions.clear<AuthSession>()
//                    call.respondRedirect("/ecommerce")
//                }
//            }
        }
    }
}