package org.example.controllers.frontend.admin

import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.origin
import io.ktor.server.request.receiveParameters
import io.ktor.server.request.userAgent
import io.ktor.server.routing.RoutingContext
import org.example.config.AuthType
import org.example.config.DynamicRouteConfig
import org.example.config.DynamicRouteFactory
import org.example.config.HttpMethodType
import org.example.config.RouteGroupConfig
import org.example.services.OtpPurpose
import org.example.di.Component
import org.example.di.Inject
import org.example.domain.models.identity.User
import org.example.domain.validations.validate
import org.example.services.AuthService
import org.example.services.ResendResult
import org.example.services.ResetPasswordResult
import org.example.services.UserService
import org.example.utils.respondHtml

@Component
class AdminUserController @Inject constructor(
    private val userService: UserService,
    private val authService: AuthService
) {

    fun registerUserRoutes(factory: DynamicRouteFactory) {

        val loginHandler: suspend RoutingContext.() -> Unit = loginHandler@{
            val formParams = call.receiveParameters()
            val email = formParams["email"].orEmpty()
            val password = formParams["password"].orEmpty()

            if (email.isEmpty() || password.isEmpty()) {
                call.respondHtml<Unit>(
                    template = "admin/auth/login",
                    status = HttpStatusCode.BadRequest,
                    errors = listOf("Email and password are required")
                )
                return@loginHandler
            }
            val tokenResponse = userService.login(
                email = email,
                password = password,
                ipAddress = call.request.origin.remoteAddress,
                deviceInfo = call.request.userAgent()
            )
            call.respondHtml(
                template = "admin/dashboard/index",
                data = tokenResponse,
                message = "Login successful"
            )
        }

        val logoutHandler: suspend RoutingContext.() -> Unit = {
            val userId = call.queryParameters["userId"].orEmpty()
            val accessToken = call.request.headers["Authorization"]?.removePrefix("Bearer ").orEmpty()

            userService.logout(userId, accessToken)
            call.respondHtml<Unit>(
                template = "admin/auth/login",
                message = "Logged out successfully"
            )
        }

        val createUserHandler: suspend RoutingContext.() -> Unit = {
            val formParams = call.receiveParameters()
            val user = User.formParameters(formParams).validate()
            val created = userService.createUser(user)

            // Send verification emails if needed
            authService.launchOTPVerification(created)

            call.respondHtml(
                template = "admin/users/create",
                data = created,
                message = "User created successfully"
            )
        }

        val updateUserHandler: suspend RoutingContext.() -> Unit = updateUserHandler@{
            val id = call.queryParameters["id"].orEmpty()
            val formParams = call.receiveParameters()

            if (id.isEmpty()) {
                call.respondHtml<Unit>(
                    template = "admin/users/list",
                    status = HttpStatusCode.BadRequest,
                    errors = listOf("User ID is required")
                )
                return@updateUserHandler
            }
            val existingUser = userService.getUser(id)
            if (existingUser == null) {
                call.respondHtml<Unit>(
                    template = "admin/users/list",
                    status = HttpStatusCode.NotFound,
                    errors = listOf("User not found")
                )
                return@updateUserHandler
            }

            val updates = User.formParameters(formParams).validate()

            val updated = userService.updateUser(id, updates)
            if (updated != null) {
                call.respondHtml(
                    template = "admin/users/update",
                    data = updated,
                    message = "User updated successfully"
                )
            } else {
                call.respondHtml<Unit>(
                    template = "admin/users/list",
                    status = HttpStatusCode.InternalServerError,
                    errors = listOf("Failed to update user")
                )
            }
        }

        val searchUsersHandler: suspend RoutingContext.() -> Unit = {
            val query = call.queryParameters["q"] ?: call.queryParameters["query"].orEmpty()
            val offset = call.queryParameters["offset"]?.toIntOrNull() ?: 0
            val limit = call.queryParameters["limit"]?.toIntOrNull() ?: 20
            val role = call.queryParameters["role"]
            val status = call.queryParameters["status"]


            val queryParams = mutableMapOf<String, String>()
            role?.let { queryParams["role"] = it }
            status?.let { queryParams["status"] = it }

            val users = if (query.isNotEmpty()) {
                userService.searchUsers(query, offset, limit)
            } else {
                userService.getUsers(offset, limit, queryParams.ifEmpty { null })
            }

            call.respondHtml(
                template = "admin/users/list",
                data = mapOf(
                    "users" to users,
                    "total" to users.size,
                    "offset" to offset,
                    "limit" to limit
                ),
                message = if (query.isNotEmpty()) {
                    "Search results for: $query"
                } else {
                    "Showing users $offset to ${offset + users.size}"
                }
            )
        }

        val getUserHandler: suspend RoutingContext.() -> Unit = getUserHandler@{
            val id = call.queryParameters["id"].orEmpty()

            if (id.isEmpty()) {
                call.respondHtml<Unit>(
                    template = "admin/users/list",
                    status = HttpStatusCode.BadRequest,
                    errors = listOf("User ID is required")
                )
                return@getUserHandler
            }
            val user = userService.getUser(id)
            if (user != null) {
                call.respondHtml(
                    template = "admin/users/detail",
                    data = user
                )
            } else {
                call.respondHtml<Unit>(
                    template = "admin/users/list",
                    status = HttpStatusCode.NotFound,
                    errors = listOf("User not found")
                )
            }
        }

        val deleteUserHandler: suspend RoutingContext.() -> Unit = deleteUserHandler@{
            val id = call.queryParameters["id"].orEmpty()

            if (id.isEmpty()) {
                call.respondHtml<Unit>(
                    template = "admin/users/list",
                    status = HttpStatusCode.BadRequest,
                    errors = listOf("User ID is required")
                )
                return@deleteUserHandler
            }
            val deleted = userService.deleteUser(id)
            if (deleted) {
                call.respondHtml<Unit>(
                    template = "admin/users/list",
                    message = "User deleted successfully"
                )
            } else {
                call.respondHtml<Unit>(
                    template = "admin/users/list",
                    status = HttpStatusCode.NotFound,
                    errors = listOf("User not found or could not be deleted")
                )
            }
        }

        val updatePasswordHandler: suspend RoutingContext.() -> Unit = updatePasswordHandler@{
            val formParams = call.receiveParameters()
            val userId = formParams["userId"].orEmpty()
            val oldPassword = formParams["oldPassword"].orEmpty()
            val newPassword = formParams["newPassword"].orEmpty()

            if (userId.isEmpty() || oldPassword.isEmpty() || newPassword.isEmpty()) {
                call.respondHtml<Unit>(
                    template = "admin/users/update",
                    status = HttpStatusCode.BadRequest,
                    errors = listOf("User ID, old password, and new password are required")
                )
                return@updatePasswordHandler
            }

            authService.updatePassword(userId, oldPassword, newPassword)
            call.respondHtml<Unit>(
                template = "admin/users/update",
                message = "Password updated successfully"
            )
        }

        val resetPasswordHandler: suspend RoutingContext.() -> Unit = resetPasswordHandler@{
            val formParams = call.receiveParameters()
            val email = formParams["email"].orEmpty()

            if (email.isEmpty()) {
                call.respondHtml<Unit>(
                    template = "admin/auth/forgot-password",
                    status = HttpStatusCode.BadRequest,
                    errors = listOf("Email is required")
                )
                return@resetPasswordHandler
            }

            val user = userService.getUserByEmail(email)
            if (user != null) {
                when (val otp = authService.resendOtp(user.id, OtpPurpose.PASSWORD_RESET)) {
                    is ResendResult.Success -> {
                        call.respondHtml<Unit>(
                            template = "admin/auth/forgot-password",
                            message = "Password reset OTP sent to your email"
                        )
                    }

                    is ResendResult.Cooldown -> {
                        call.respondHtml<Unit>(
                            template = "admin/auth/forgot-password",
                            status = HttpStatusCode.TooManyRequests,
                            errors = listOf("Please wait ${otp.seconds} seconds before requesting another OTP")
                        )
                    }

                    is ResendResult.UserNotFound -> {
                        call.respondHtml<Unit>(
                            template = "admin/auth/forgot-password",
                            status = HttpStatusCode.NotFound,
                            errors = listOf("User not found")
                        )
                    }
                }
            } else {
                // Don't reveal if user exists for security
                call.respondHtml<Unit>(
                    template = "admin/auth/forgot-password",
                    message = "If the email exists, a password reset OTP has been sent"
                )
            }
        }

        val verifyResetPasswordHandler: suspend RoutingContext.() -> Unit = verifyResetPasswordHandler@{
            val formParams = call.receiveParameters()
            val email = formParams["email"].orEmpty()
            val otp = formParams["otp"].orEmpty()
            val newPassword = formParams["newPassword"].orEmpty()

            if (email.isEmpty() || otp.isEmpty() || newPassword.isEmpty()) {
                call.respondHtml<Unit>(
                    template = "admin/auth/reset-password",
                    status = HttpStatusCode.BadRequest,
                    errors = listOf("Email, OTP, and new password are required")
                )
                return@verifyResetPasswordHandler
            }

            when (val result = authService.resetPassword(email, otp, newPassword)) {
                is ResetPasswordResult.Success -> {
                    call.respondHtml<Unit>(
                        template = "admin/auth/login",
                        message = "Password reset successfully. Please login with your new password."
                    )
                }

                is ResetPasswordResult.Failure -> {
                    call.respondHtml<Unit>(
                        template = "admin/auth/reset-password",
                        status = HttpStatusCode.BadRequest,
                        errors = listOf(result.message)
                    )
                }

                is ResetPasswordResult.UserNotFound -> {
                    call.respondHtml<Unit>(
                        template = "admin/auth/reset-password",
                        status = HttpStatusCode.NotFound,
                        errors = listOf("User not found")
                    )
                }
            }
        }

        val toggle2FAHandler: suspend RoutingContext.() -> Unit = toggle2FAHandler@{
            val userId = call.queryParameters["userId"].orEmpty()
            val enabled = call.queryParameters["enabled"]?.toBoolean() ?: false

            if (userId.isEmpty()) {
                call.respondHtml<Unit>(
                    template = "admin/users/list",
                    status = HttpStatusCode.BadRequest,
                    errors = listOf("User ID is required")
                )
                return@toggle2FAHandler
            }
            authService.update2FAStatus(userId, enabled)
            call.respondHtml<Unit>(
                template = "admin/users/detail",
                message = "2FA ${if (enabled) "enabled" else "disabled"} successfully"
            )
        }

        val bulkDeleteUsersHandler: suspend RoutingContext.() -> Unit = bulkDeleteUsersHandler@{
            val ids = call.queryParameters.getAll("ids") ?: emptyList()

            if (ids.isEmpty()) {
                call.respondHtml<Unit>(
                    template = "admin/users/list",
                    status = HttpStatusCode.BadRequest,
                    errors = listOf("User IDs are required")
                )
                return@bulkDeleteUsersHandler
            }
            val results = mutableMapOf<String, Boolean>()
            for (id in ids) {
                results[id] = userService.deleteUser(id)
            }
            val successCount = results.count { it.value }

            call.respondHtml(
                template = "admin/users/list",
                data = results,
                message = "Deleted $successCount of ${ids.size} users"
            )
        }

        val verify2FAHandler: suspend RoutingContext.() -> Unit = verify2FAHandler@{
            val formParams = call.receiveParameters()
            val userId = formParams["userId"].orEmpty()
            val otp = formParams["otp"].orEmpty()

            if (userId.isEmpty() || otp.isEmpty()) {
                call.respondHtml<Unit>(
                    template = "admin/auth/verify-2fa",
                    status = HttpStatusCode.BadRequest,
                    errors = listOf("User ID and OTP are required")
                )
                return@verify2FAHandler
            }
            val tokenResponse = authService.verify2FAAndLogin(
                userId = userId,
                otp = otp,
                ipAddress = call.request.origin.remoteAddress,
                deviceInfo = call.request.userAgent()
            )
            call.respondHtml(
                template = "admin/dashboard/index",
                data = tokenResponse,
                message = "Login successful"
            )
        }

        val group = RouteGroupConfig(
            prefix = "/users"
        )

        val routes = listOf(
            // Authentication routes
            DynamicRouteConfig(
                path = "/login",
                methods = setOf(HttpMethodType.POST),
                handler = loginHandler,
                name = "AdminLogin",
                requiresAuth = false,
                authType = AuthType.NONE
            ),
            DynamicRouteConfig(
                path = "/logout",
                methods = setOf(HttpMethodType.POST),
                handler = logoutHandler,
                name = "AdminLogout",
                requiresAuth = true,
                authType = AuthType.SESSION
            ),
            DynamicRouteConfig(
                path = "/verify-2fa",
                methods = setOf(HttpMethodType.POST),
                handler = verify2FAHandler,
                name = "Verify2FA",
                requiresAuth = false,
                authType = AuthType.NONE
            ),

            // User management routes
            DynamicRouteConfig(
                path = "/new",
                methods = setOf(HttpMethodType.POST),
                handler = createUserHandler,
                name = "CreateUser",
                requiresAuth = true,
                authType = AuthType.SESSION
            ),
            DynamicRouteConfig(
                path = "/update",
                methods = setOf(HttpMethodType.PUT, HttpMethodType.POST),
                handler = updateUserHandler,
                name = "UpdateUser",
                requiresAuth = true,
                authType = AuthType.SESSION
            ),
            DynamicRouteConfig(
                path = "/search",
                methods = setOf(HttpMethodType.GET),
                handler = searchUsersHandler,
                name = "SearchUsers",
                requiresAuth = true,
                authType = AuthType.SESSION
            ),
            DynamicRouteConfig(
                path = "/detail",
                methods = setOf(HttpMethodType.GET),
                handler = getUserHandler,
                name = "GetUser",
                requiresAuth = true,
                authType = AuthType.SESSION
            ),
            DynamicRouteConfig(
                path = "/delete",
                methods = setOf(HttpMethodType.DELETE, HttpMethodType.POST),
                handler = deleteUserHandler,
                name = "DeleteUser",
                requiresAuth = true,
                authType = AuthType.SESSION
            ),
            DynamicRouteConfig(
                path = "/bulk-delete",
                methods = setOf(HttpMethodType.POST),
                handler = bulkDeleteUsersHandler,
                name = "BulkDeleteUsers",
                requiresAuth = true,
                authType = AuthType.SESSION
            ),

            // Password management routes
            DynamicRouteConfig(
                path = "/update-password",
                methods = setOf(HttpMethodType.POST),
                handler = updatePasswordHandler,
                name = "UpdatePassword",
                requiresAuth = true,
                authType = AuthType.SESSION
            ),
            DynamicRouteConfig(
                path = "/forgot-password",
                methods = setOf(HttpMethodType.POST),
                handler = resetPasswordHandler,
                name = "ForgotPassword",
                requiresAuth = false,
                authType = AuthType.NONE
            ),
            DynamicRouteConfig(
                path = "/reset-password",
                methods = setOf(HttpMethodType.POST),
                handler = verifyResetPasswordHandler,
                name = "ResetPassword",
                requiresAuth = false,
                authType = AuthType.NONE
            ),

            // 2FA management route
            DynamicRouteConfig(
                path = "/toggle-2fa",
                methods = setOf(HttpMethodType.POST),
                handler = toggle2FAHandler,
                name = "Toggle2FA",
                requiresAuth = true,
                authType = AuthType.SESSION
            )
        )
        factory.registerGroup("UserRoutesGroup", group, routes)
    }
}