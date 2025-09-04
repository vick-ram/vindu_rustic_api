package org.example.controllers

import com.google.gson.Gson
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.webSocket
import io.ktor.util.*
import io.ktor.websocket.*
import org.example.domain.models.LoginCredentials
import org.example.domain.models.User
import org.example.plugins.AuthenticationException
import org.example.services.UserService
import org.example.utils.IncomingMessage
import org.example.utils.Json
import org.example.utils.OutgoingMessage
import org.example.utils.WebsocketConnectionManager
import org.example.utils.generateResponse
import org.example.utils.respondApi
import java.lang.Exception

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
            get {
                val offset = call.request.queryParameters["offset"]?.toIntOrNull() ?: 0
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 10
                val queryParams = call.request.queryParameters.toMap().mapValues { it.value.firstOrNull() ?: "" }
                    .filterKeys { it != "offset" && it != "limit" }
                val users = userService.getUsers(offset, limit, queryParams)
                call.respondApi(
                    data = users,
                    message = "Users fetched successfully"
                )
            }


            get("{id}") {
                val id = call.parameters["id"] ?: ""
                val user = userService.getUser(id)
                call.respondApi(
                    data = user,
                    message = "User fetched successfully"
                )

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

            get("search/{query}") {
                val query = call.parameters["query"] ?: "the"
                val offset = call.request.queryParameters["offset"]?.toIntOrNull() ?: 0
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 50
                val users = userService.searchUsers(query, offset, limit)
                call.respondApi(
                    data = users,
                    message = "Users fetched successfully"
                )
            }

            delete("{id}") {
                val id = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
                val deleted = userService.deleteUser(id)
                if (deleted) {
                    call.respondApi<Unit>(
                        status = HttpStatusCode.NoContent,
                        message = "User deleted successfully"
                    )
                }
            }

            webSocket("support") {

                println("Client connected: $this")

                // Send a welcome message when a client connects
                val welcomeMessage =
                    OutgoingMessage("Howdy! Welcome to The Rustic Forge. I'm your virtual assistant. What can I help you with today?")
                send(Json.encodeToString(welcomeMessage))

                for (frame in incoming) {
                    when (frame) {
                        is Frame.Text -> {
                            try {
                                val receivedText = frame.readText()
                                println("Received: $receivedText")
                                // Parse the incoming JSON message
                                val incomingMessage = Json.decodeFromString<IncomingMessage>(receivedText)

                                // Generate response using bot
                                val botReply = generateResponse(incomingMessage.message)

                                // Create and send the response message
                                val outgoingMessage = OutgoingMessage(botReply)
                                send(Json.encodeToString(outgoingMessage))
                            } catch (e: Exception) {
                                e.printStackTrace()
                                val errorMessage = OutgoingMessage("Whoops! The forge is a bit smoky today. Could you try again?")
                                send(Json.encodeToString(errorMessage))
                            }
                        }
                        else -> {
                            println("Received non-text frame: $frame")
                        }
                    }
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
