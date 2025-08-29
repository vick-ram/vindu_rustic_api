package org.example.controllers

import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.*
import io.ktor.websocket.*
import org.example.domain.models.LoginCredentials
import org.example.domain.models.User
import org.example.plugins.AuthenticationException
import org.example.services.UserService
import org.example.utils.OpenApiGet
import org.example.utils.WebsocketConnectionManager
import org.example.utils.openApiGet
import org.example.utils.respondApi

class UserController(
    private val userService: UserService
) {
    fun Route.routes(issuer: String, audience: String, secret: String) {
        route("/users/") {
            post("login") {
                val credentials = call.receive<LoginCredentials>().validate()
                val res = userService.login(credentials.email, credentials.password, issuer, audience, secret)
                call.respondApi(
                    status = HttpStatusCode.OK,
                    data = res,
                    message = "User logged in successfully"
                )
            }

            authenticate("auth-jwt") {
                post("logout") {
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
            openApiGet<List<User>>("") @OpenApiGet(
                path = "",
                summary = "Fetch all users",
                description = "Retrieves a list of users from database",
                tags = ["Users"]
            ) {
                val offset = request.queryParameters["offset"]?.toIntOrNull() ?: 0
                val limit = request.queryParameters["limit"]?.toIntOrNull() ?: 10
                val queryParams = request.queryParameters.toMap().mapValues { it.value.firstOrNull() ?: "" }
                    .filterKeys { it != "offset" && it != "limit" }
                userService.getUsers(offset, limit, queryParams)
            }


            openApiGet<User>("{id}") {
                val id = parameters["id"] ?: ""
                userService.getUser(id) as User
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

            openApiGet<List<User>>("search/{query}") {
                val query = parameters["query"] ?: "the"
                val offset = request.queryParameters["offset"]?.toIntOrNull() ?: 0
                val limit = request.queryParameters["limit"]?.toIntOrNull() ?: 50
                userService.searchUsers(query, offset, limit)
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

            // Chat endpoint
//            authenticate("auth-jwt") {
//                webSocket("chat") {
//                    val principal = call.principal<JWTPrincipal>()
//                    val userId = principal?.subject ?: return@webSocket close()
//                    val email = principal.getClaim("email", String::class) ?: ""
//
//                    val sessionId = connectionManager.addUser(userId, email, this)
//
//                    try {
//                        for (frame in incoming) {
//                            when (frame) {
//                                is Frame.Text -> {
//                                    val message = frame.readText()
//                                    sendPrivateMessage(connectionManager, sessionId, "", message)
//                                }
//
//                                else -> {}
//                            }
//                        }
//                    } finally {
//                        connectionManager.removeUser(sessionId)
//                    }
//                }
//            }
        }
    }
}

suspend fun sendPrivateMessage(
    connectionManager: WebsocketConnectionManager,
    sessionId: String,
    recipientId: String,
    message: String
) {
    val recipients = connectionManager.getUsersByUserId(recipientId)
    recipients.forEach { recipient ->
        recipient.socket.send(
            Frame.Text(
                """{
            "from": "$sessionId",
            "message": "$message"
        }""".trimIndent()
            )
        )
    }
}
