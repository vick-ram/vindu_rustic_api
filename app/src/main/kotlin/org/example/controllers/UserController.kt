package org.example.controllers

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
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
import org.example.plugins.AuthenticationException
import org.example.services.UserService
import org.example.utils.respondApi

class UserController(
    private val userService: UserService
) {
    fun Route.routes(issuer: String, audience: String, secret: String) {
        route("/users") {
            post("/login") {
                val credentials = call.receive<LoginCredentials>().validate()
                val res = userService.login(credentials.email, credentials.password, issuer, audience, secret)
                call.respondApi(
                    status = HttpStatusCode.OK,
                    data = res,
                    message = "User logged in successfully"
                )
            }

            authenticate("auth-jwt") {
                post("/logout") {
                    val token =
                        call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: throw AuthenticationException(
                            "No token was passed in headers"
                        )
                    val result = userService.logout(token)
                    if (result) {
                        call.respondApi(
                            status = HttpStatusCode.OK,
                            data = null,
                            message = "User logged out successfully"
                        )
                    } else {
                        throw AuthenticationException("Login failed")
                    }
                }
            }
            get {
                val offset = call.request.queryParameters["offset"]?.toIntOrNull() ?: 0
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 10
                val queryParams = call.request.queryParameters.toMap().mapValues { it.value.firstOrNull() ?: "" }
                    .filterKeys { it != "offset" && it != "limit" }

                val users = userService.getUsers(offset, limit, queryParams)

                call.respondApi(
                    status = HttpStatusCode.OK,
                    data = users,
                    message = "Users fetched successfully"
                )
            }

            get("{id}") {
                val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                val user = userService.getUser(id)

                if (user != null) {
                    call.respondApi(
                        status = HttpStatusCode.OK,
                        data = user,
                        message = "User read successfully"
                    )
                } else {
                    throw NotFoundException("User not found")
                }
            }

            post {
                val newUser = call.receive<User>().validate()
                val created = userService.createUser(newUser)

                call.respondApi(
                    status = HttpStatusCode.Created,
                    data = created,
                    message = "User created successfully"
                )
            }

            put("{id}") {
                val id = call.parameters["id"] ?: return@put call.respond(HttpStatusCode.BadRequest)
                val updatedUser = call.receive<User>()
                val updated = userService.updateUser(id, updatedUser)
                if (updated != null) {
                    call.respondApi(
                        status = HttpStatusCode.Accepted,
                        data = updated,
                        message = "User updated successfully"
                    )
                }
            }

            delete("{id}") {
                val id = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
                val deleted = userService.deleteUser(id)
                if (deleted) {
                    call.respondApi(
                        status = HttpStatusCode.NoContent,
                        data = null,
                        message = "User deleted successfully"
                    )
                } else {
                    throw NotFoundException("User not found")
                }
            }
        }
    }
}