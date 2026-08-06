package org.example.controllers.frontend.ecommerce

import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.origin
import io.ktor.server.request.receiveParameters
import io.ktor.server.request.userAgent
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.sessions.clear
import io.ktor.server.sessions.sessions
import io.ktor.server.sessions.set
import org.example.di.Component
import org.example.di.Inject
import org.example.domain.models.identity.User
import org.example.exceptions.AuthenticationException
import org.example.exceptions.NotFoundException
import org.example.exceptions.TwoFactorRequiredException
import org.example.plugins.AuthSession
import org.example.services.UserService
import org.example.utils.respondHtml

@Component
class AuthController @Inject constructor(
    private val userService: UserService
) {
    fun Route.authRoutes() {
        route("/signup") {
            get {
                call.respondHtml(
                    template = "auth/signup",
                    data = emptySignupForm(),
                    mapData = mutableMapOf("formData" to emptySignupForm())
                )
            }

            post {
                val formData = User.formParameters(call.receiveParameters())
                if (userService.getUserByEmail(formData.email) != null) {
                    return@post call.respondHtml(
                        template = "auth/signup",
                        status = HttpStatusCode.Conflict,
                        data = formData,
                        errors = listOf("An account with that email already exists"),
                        mapData = mutableMapOf("formData" to formData)
                    )
                }

                userService.createUser(formData)
                call.respondRedirect("/signin?registered=true")
            }
        }

        route("/signin") {
            get {
                call.respondHtml<Unit>(
                    template = "auth/signin",
                    message = call.request.queryParameters["registered"]
                        ?.takeIf { it == "true" }
                        ?.let { "Account created. Please sign in." },
                    mapData = mutableMapOf(
                        "redirectUrl" to (call.request.queryParameters["redirect"] ?: "/ecommerce")
                    )
                )
            }

            post {
                val parameters = call.receiveParameters()
                val email = parameters["email"].orEmpty()
                val password = parameters["password"].orEmpty()
                val redirectUrl = parameters["redirectUrl"].takeRedirectOrDefault()

                if (email.isBlank() || password.isBlank()) {
                    return@post call.respondHtml<Unit>(
                        template = "auth/signin",
                        status = HttpStatusCode.BadRequest,
                        errors = listOf("Email and password are required"),
                        mapData = mutableMapOf("redirectUrl" to redirectUrl)
                    )
                }

                try {
                    userService.login(
                        email = email,
                        password = password,
                        ipAddress = call.request.origin.remoteAddress,
                        deviceInfo = call.request.userAgent()
                    )
                    val user = userService.getUserByEmail(email)
                        ?: throw NotFoundException("User not found")

                    call.sessions.set(AuthSession(user.id, user.email))
                    call.respondRedirect(redirectUrl)
                } catch (exception: TwoFactorRequiredException) {
                    call.respondHtml<Unit>(
                        template = "auth/verify",
                        status = HttpStatusCode.Accepted,
                        message = "Enter the verification code sent to you.",
                        mapData = mutableMapOf("userId" to exception.userId)
                    )
                } catch (exception: AuthenticationException) {
                    call.respondHtml<Unit>(
                        template = "auth/signin",
                        status = HttpStatusCode.Unauthorized,
                        errors = listOf(exception.message ?: "Invalid email or password"),
                        mapData = mutableMapOf("redirectUrl" to redirectUrl)
                    )
                } catch (exception: NotFoundException) {
                    call.respondHtml<Unit>(
                        template = "auth/signin",
                        status = HttpStatusCode.Unauthorized,
                        errors = listOf("Invalid email or password"),
                        mapData = mutableMapOf("redirectUrl" to redirectUrl)
                    )
                }
            }
        }

        get("/forgot-password") {
            call.respondHtml<Unit>(template = "auth/forgot-password")
        }

        post("/logout") {
            call.sessions.clear<AuthSession>()
            call.respondRedirect("/ecommerce")
        }
    }

    private fun emptySignupForm() = User(
        firstName = "",
        lastName = "",
        email = "",
        password = "",
        phoneNumber = ""
    )

    private fun String?.takeRedirectOrDefault(): String =
        this?.takeIf { it.startsWith("/") && !it.startsWith("//") } ?: "/ecommerce"
}
