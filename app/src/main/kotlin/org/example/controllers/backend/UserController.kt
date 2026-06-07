package org.example.controllers.backend

import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.*
import io.ktor.websocket.*
import org.example.domain.models.identity.LoginCredentials
import org.example.domain.models.identity.User
import org.example.plugins.AuthenticationException
import org.example.services.UserService
import org.example.config.AuthType
import org.example.config.DynamicRouteConfig
import org.example.config.DynamicRouteFactory
import org.example.config.HttpMethodType
import org.example.config.RouteGroupConfig
import org.example.utils.IncomingMessage
import org.example.utils.Json
import org.example.utils.OutgoingMessage
import org.example.utils.WebsocketConnectionManager
import org.example.utils.generateResponse
import org.example.utils.respondApi

class UserController(
    private val userService: UserService,
) {
    fun registerUserRoutes(issuer: String, audience: String, secret: String, factory: DynamicRouteFactory) {
        val loginHandler: suspend RoutingContext.() -> Unit = {
            val credentials = call.receive<LoginCredentials>().validate()
            val res = userService.login(credentials.email, credentials.password, issuer, audience, secret)
            call.respondApi(
                status = HttpStatusCode.OK,
                data = res,
                message = "User logged in successfully"
            )
        }

        val logoutHandler: suspend RoutingContext.() -> Unit = {
            val token = call.request.headers["Authorization"]?.removePrefix("Bearer ")
                ?: throw AuthenticationException("No token was passed in headers")
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

        val getUsersHandler: suspend RoutingContext.() -> Unit = {
            val offset = call.request.queryParameters["offset"]?.toIntOrNull() ?: 0
            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 10
            val queryParams = call.request.queryParameters.toMap().mapValues { it.value.firstOrNull() ?: "" }
                .filterKeys { it != "offset" && it != "limit" }
            val query = call.request.queryParameters["q"]
            val users = if (query != null) {
                userService.searchUsers(query, offset, limit)
            } else {
                userService.getUsers(offset, limit, queryParams)
            }
            call.respondApi(data = users, message = "Users fetched successfully")
        }

        val getUserHandler: suspend RoutingContext.() -> Unit = {
            val id = call.parameters["id"] ?: ""
            val user = userService.getUser(id)
            call.respondApi(data = user, message = "User fetched successfully")
        }

        val createUserHandler: suspend RoutingContext.() -> Unit = {
            val newUser = call.receive<User>().validate()
            val created = userService.createUser(newUser)
            call.respondApi(status = HttpStatusCode.Created, data = created, message = "User created successfully")
        }

        val updateUserHandler: suspend RoutingContext.() -> Unit = updateUserHandler@{
            val id = call.parameters["id"] ?: return@updateUserHandler call.respond(HttpStatusCode.BadRequest)
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

        val supportHandler: suspend DefaultWebSocketSession.() -> Unit = {
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

        val deleteUserHandler: suspend RoutingContext.() -> Unit = deleteUserHandler@{
            val id = call.parameters["id"] ?: return@deleteUserHandler call.respond(HttpStatusCode.BadRequest)
            val deleted = userService.deleteUser(id)
            if (deleted) {
                call.respondApi<Unit>(
                    status = HttpStatusCode.NoContent,
                    message = "User deleted successfully"
                )
            }
        }

        val group = RouteGroupConfig(
            prefix = "/api/users",
            version = "1.0.0"
        )

        val routes = listOf(
            DynamicRouteConfig(
                path = "/login",
                methods = setOf(HttpMethodType.POST),
                handler = loginHandler,
                name = "UserLogin",
                requiresAuth = false
            ),
            DynamicRouteConfig(
                path = "/logout",
                methods = setOf(HttpMethodType.POST),
                handler = logoutHandler,
                requiresAuth = true,
                authType = AuthType.JWT,
                name = "Logout user",
                metadata = mapOf(
                    "tag" to "user"
                )
            ),
            DynamicRouteConfig(
                path = "",
                methods = setOf(HttpMethodType.GET),
                handler = getUsersHandler,
                name = "Get all users"
            ),
            DynamicRouteConfig(
                path = "/{id}",
                methods = setOf(HttpMethodType.GET),
                handler = getUserHandler,
                name = "Get user by id"
            ),
            DynamicRouteConfig(
                path = "",
                methods = setOf(HttpMethodType.POST),
                handler = createUserHandler,
                name = "Create new user"
            ),
            DynamicRouteConfig(
                path = "/{id}",
                methods = setOf(HttpMethodType.PUT),
                handler = updateUserHandler,
                name = "Update user"
            ),
            DynamicRouteConfig(
                path = "/{id}",
                methods = setOf(HttpMethodType.DELETE),
                handler = deleteUserHandler,
                name = "DeleteUser"
            ),
        )

        factory.registerGroup("UserRoutesGroup", group, routes)
        factory.addWebsocketRoute(
            path = "/api/users/support",
            handler = supportHandler
        )
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
