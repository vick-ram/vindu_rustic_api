package org.example.controllers.backend

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.util.toMap
import org.example.domain.models.identity.Role
import org.example.services.RoleService
import org.example.utils.respondApi

class RoleController(private val roleService: RoleService) {
    fun Route.routes() {
        route("/roles/") {
            post {
                val roleRequest = call.receive<Role>().validate()
                val newRole = roleService.createRole(roleRequest)
                call.respondApi(
                    status = HttpStatusCode.Created,
                    data = newRole,
                    message = "Role created successfully"
                )
            }

            put("{id}") {
                val roleId = call.parameters["id"] ?: ""
                val roleUpdateRequest = call.receive<Role>().validate()
                val updatedRole = roleService.updateRole(roleId, roleUpdateRequest)
                call.respondApi(
                    status = HttpStatusCode.Accepted,
                    data = updatedRole,
                    message = "Role updated successfully"
                )
            }

            get("{id}") {
                val roleId = call.parameters["id"] ?: ""
                val readRole = roleService.getRole(roleId)
                call.respondApi(
                    data = readRole,
                    message = "Role fetched successfully"
                )
            }

            get {
                val offset = call.request.queryParameters["offset"]?.toIntOrNull() ?: 0
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 10
                val queryParams = call.request.queryParameters.toMap().mapValues { it.value.firstOrNull() ?: "" }
                    .filterKeys { it != "offset" && it != "limit" }

                val readRoles = roleService.getRoles(offset, limit, queryParams)
                call.respondApi(
                    data = readRoles,
                    message = "Roles fetched successfully"
                )
            }

            delete("{id}") {
                val roleId = call.parameters["id"] ?: ""
                val roleDeleted = roleService.deleteRole(roleId)
                if (roleDeleted) {
                    call.respondApi<Unit>(
                        status = HttpStatusCode.NoContent,
                        message = "Role deleted successfully"
                    )
                }
            }
        }
    }
}