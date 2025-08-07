package org.example.plugins

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import org.example.utils.ApiResponse

fun Application.configureStatusPages() {
    install(StatusPages) {
        exception<AuthenticationException> { call, cause ->
            call.respond(
                ApiResponse.error<Unit>(
                    status = HttpStatusCode.Unauthorized,
                    msg = cause.message ?: "Unauthorized",
                )
            )
        }

        exception<ForbiddenException> { call, cause ->
            call.respond(
                ApiResponse.error<Unit>(
                    status = HttpStatusCode.Forbidden,
                    msg = cause.message ?: "Forbidden",
                )
            )
        }

        exception<NotFoundException> {call, cause ->
            call.respond(
                ApiResponse.error<Unit>(
                    status = HttpStatusCode.NotFound,
                    msg = cause.message ?: "Not found",
                )
            )
        }

        exception<BadRequestException> {call, cause ->
            call.respond(
                ApiResponse.error<Unit>(
                    status = HttpStatusCode.NotFound,
                    msg = cause.message ?: "Bad Request",
                )
            )
        }

        exception<ValidationException> {call, cause ->
            call.respond(
                ApiResponse.error<Unit>(
                    status = HttpStatusCode.BadRequest,
                    msg = cause.message ?: "Validation error",
                )
            )
        }

        exception<ConflictException> {call, cause ->
            call.respond(
                ApiResponse.error<Unit>(
                    status = HttpStatusCode.NotFound,
                    msg = cause.message ?: "Not found",
                )
            )
        }
    }
}

class NotFoundException(message: String) : Exception(message)
class AuthenticationException(override val message: String?) : Exception(message)
class ForbiddenException(override val message: String?) : Exception(message)
class BadRequestException(message: String) : Exception(message)
class ConflictException(message: String) : Exception(message)
class ValidationException(message: String) : Exception(message) {
    constructor(errors: List<String>) : this(errors.joinToString(", "))
}