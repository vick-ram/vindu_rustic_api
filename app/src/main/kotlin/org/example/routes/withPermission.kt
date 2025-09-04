package org.example.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.createRouteScopedPlugin
import io.ktor.server.auth.AuthenticationChecked
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.Route
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions
import org.example.data.db.entities.UserEntity
import org.example.data.db.tables.UserTable
import org.example.plugins.AuthSession
import org.example.plugins.ForbiddenException
import org.example.services.RoleService
import org.example.services.UserService
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
fun Route.requireAuth(role: String, userService: UserService, roleService: RoleService, build: () -> Route) {

    val randomUuid = Uuid.random()
    val uniqueId = randomUuid.toString().take(4)
    val roleSessionPlugin =
        createRouteScopedPlugin(name = "rolePlugin_$uniqueId", createConfiguration = ::PluginConfiguration) {
            on(AuthenticationChecked) { call ->
                val userService = pluginConfig.userService
                val roleService = pluginConfig.roleService
                val role = pluginConfig.roleName

                val session = call.sessions.get<AuthSession>()
                if (session == null) {
                    call.respondRedirect("/signin")
                    return@on
                }

                val user = userService?.getUser(session.userId)
                val userRole = user?.roleId?.let { roleService?.getRole(it) }?.name

                if (userRole != role) {
                    call.respond(HttpStatusCode.Forbidden, "Access denied")
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
}

class PluginConfiguration {
    var roleName: String = ""
    var userService: UserService? = null
    var roleService: RoleService? = null
}
