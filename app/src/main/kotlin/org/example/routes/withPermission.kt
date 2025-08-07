package org.example.routes

import io.ktor.server.application.createRouteScopedPlugin
import io.ktor.server.auth.AuthenticationChecked
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.routing.Route
import org.example.data.db.entities.UserEntity
import org.example.data.db.tables.UserTable
import org.example.plugins.ForbiddenException

fun Route.withPermission(permissionName: String, build: () -> Route) {
    install(permissionPlugin) {
        this.permissionName = permissionName
    }
    build()
}

val permissionPlugin = createRouteScopedPlugin(name = "permissionPlugin", createConfiguration = ::PluginConfiguration) {
    val permissionName = pluginConfig.permissionName

    pluginConfig.apply {
        on(AuthenticationChecked) { call ->
            val principal = call.principal<JWTPrincipal>()
            val email = principal?.payload?.getClaim("email")?.asString() ?: ""
            val user = UserEntity.find { UserTable.email.eq(email) }.firstOrNull()
            val hasPermissions = user?.role?.permissions?.any { it.name == permissionName }
            if (hasPermissions != true) {
                throw ForbiddenException("You do not have permission to access this resource.")
            }
        }
    }
}

class PluginConfiguration {
    var permissionName: String = ""
}
