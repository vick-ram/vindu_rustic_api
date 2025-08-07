package org.example.controllers

import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.NotFoundException
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.util.toMap
import org.example.domain.models.LoginCredentials
import org.example.domain.models.User
import org.example.domain.repo.UserRepository
import org.example.routes.withPermission
import org.example.utils.ApiResponse

class UserController(
    private val userRepo: UserRepository
) {
    fun Route.routes(issuer: String, audience: String, secret: String) {
        route("/users") {
            withPermission("") {
                post("/login") {
                    val credentials = call.receive<LoginCredentials>()
                    val res = userRepo.login(credentials.email, credentials.password, issuer, audience, secret)
                    call.respond(
                        ApiResponse.success(
                            status = HttpStatusCode.OK,
                            data = res,
                            message = "User logged in successfully"
                        )
                    )
                }
            }

            post("/logout") {
                val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: return@post call.respond(
                    HttpStatusCode.Unauthorized
                )
                val result = userRepo.logout(token)
                if (result) {
                    call.respond(
                        ApiResponse.success(
                            status = HttpStatusCode.OK,
                            data = null,
                            message = "User logged out successfully"
                        )
                    )
                } else {
                    call.respond(HttpStatusCode.BadRequest, "Logout failed")
                }
            }
            get {
                val offset = call.request.queryParameters["offset"]?.toIntOrNull() ?: 0
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 10
                val queryParams = call.request.queryParameters.toMap().mapValues { it.value.firstOrNull() ?: "" }
                    .filterKeys { it != "offset" && it != "limit" }
                val users = userRepo.readAll(offset, limit, queryParams)
                call.respond(
                    ApiResponse.success(
                        status = HttpStatusCode.OK,
                        data = users,
                        message = "Users fetched successfully"
                    )
                )
            }

            get("{id}") {
                val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                val user = userRepo.read(id)

                if (user != null) {
                    call.respond(
                        ApiResponse.success(
                            status = HttpStatusCode.OK,
                            data = user,
                            message = "User read successfully"
                        )
                    )
                } else {
                    throw NotFoundException("User not found")
                }
            }

            post {
                val newUser = call.receive<User>()
                val created = userRepo.create(newUser)
                call.respond(
                    ApiResponse.success(
                        status = HttpStatusCode.Created,
                        data = created,
                        message = "User created successfully"
                    )
                )
            }

            put("{id}") {
                val id = call.parameters["id"] ?: return@put call.respond(HttpStatusCode.BadRequest)
                val updatedUser = call.receive<User>()
                val updated = userRepo.update(id, updatedUser)
                if (updated != null) {
                    call.respond(
                        ApiResponse.success(
                            status = HttpStatusCode.Accepted,
                            data = updated,
                            message = "User updated successfully"
                        )
                    )
                }
            }

            delete("{id}") {
                val id = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
                val deleted = userRepo.delete(id)
                if (deleted) {
                    call.respond(
                        ApiResponse.success(
                            status = HttpStatusCode.NoContent,
                            data = null,
                            message = "User deleted successfully"
                        )
                    )
                } else {
                    throw NotFoundException("User not found")
                }
            }
        }
    }
}