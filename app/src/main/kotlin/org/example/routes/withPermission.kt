package org.example.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.createRouteScopedPlugin
import io.ktor.server.auth.AuthenticationChecked
import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.Route
import io.ktor.server.sessions.clear
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions
import org.example.plugins.AuthSession
import org.example.services.RoleService
import org.example.services.UserService
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

fun Route.requireAuth(role: String, userService: UserService, roleService: RoleService, build: () -> Route): Route {

    val randomUuid = Uuid.random()
    val uniqueId = randomUuid.toString().take(4)
    val roleSessionPlugin =
        createRouteScopedPlugin(name = "rolePlugin_$uniqueId", createConfiguration = ::PluginConfiguration) {
            on(AuthenticationChecked) { call ->
                val uService = pluginConfig.userService ?: return@on
                val rService = pluginConfig.roleService ?: return@on
                val requiredRole = pluginConfig.roleName

                val session = call.sessions.get<AuthSession>()
                if (session == null) {
                    call.respondRedirect("/signin")
                    return@on
                }

                val user = uService.getUser(session.userId)
                if (user == null) {
                    call.sessions.clear<AuthSession>()
                    call.respondRedirect("/signin")
                    return@on
                }

                val hasRole = rService.hasRoleByName(user.id, requiredRole)
                if (!hasRole) {
                    call.respond(
                        HttpStatusCode.Forbidden,
                        "Access denied. Required role: $requiredRole"
                    )
                    return@on
                }
            }
        }

    install(roleSessionPlugin) {
        this.roleName = role
        this.userService = userService
        this.roleService = roleService
    }
    build()
    return this
}

class PluginConfiguration {
    var roleName: String = ""
    var userService: UserService? = null
    var roleService: RoleService? = null
}
