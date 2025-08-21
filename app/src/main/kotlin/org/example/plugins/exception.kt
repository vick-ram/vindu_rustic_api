package org.example.plugins

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.statuspages.StatusPages
import org.example.utils.respondApi
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.postgresql.util.PSQLException
import java.sql.SQLException

fun Application.configureStatusPages() {
    install(StatusPages) {
        // Auth-related
        exception<AuthenticationException> { call, cause ->
            call.respondApi<Unit>(
                status = HttpStatusCode.Unauthorized,
                message = cause.message ?: "Unauthorized",
            )
        }

        exception<ForbiddenException> { call, cause ->
            call.respondApi<Unit>(
                status = HttpStatusCode.Forbidden,
                message = cause.message ?: "Forbidden",
            )
        }

        // Not found
        exception<NotFoundException> { call, cause ->
            call.respondApi<Unit>(
                status = HttpStatusCode.NotFound,
                message = cause.message ?: "Not found",
            )
        }

        exception<NoSuchElementException> { call, cause ->
            call.respondApi<Unit>(
                status = HttpStatusCode.NotFound,
                message = cause.message ?: "Not found",
            )
        }

        // Bad request / validation
        exception<BadRequestException> { call, cause ->
            call.respondApi<Unit>(
                status = HttpStatusCode.BadRequest,
                message = cause.message ?: "Bad Request",
            )
        }

        exception<IllegalArgumentException> { call, cause ->
            call.respondApi<Unit>(
                status = HttpStatusCode.BadRequest,
                message = cause.message ?: "Bad Request",
            )
        }

        exception<NumberFormatException> { call, cause ->
            call.respondApi<Unit>(
                status = HttpStatusCode.BadRequest,
                message = cause.message ?: "Invalid number format",
            )
        }

        exception<ValidationException> { call, cause ->
            call.respondApi<Unit>(
                status = HttpStatusCode.BadRequest,
                message = "Validation failed",
                errors = cause.errors
            )
        }

        // Conflict
        exception<ConflictException> { call, cause ->
            call.respondApi<Unit>(
                status = HttpStatusCode.Conflict,
                message = cause.message ?: "Conflict",
            )
        }

        // Database-related exceptions
        exception<ExposedSQLException> { call, cause ->
            val state = (try {
                cause.sqlState
            } catch (_: Throwable) {
                null
            })
                ?: (cause.cause as? SQLException)?.sqlState
            val (status, message) = mapSqlState(state, cause.message)
            call.respondApi<Unit>(status = status, message = message)
        }

        exception<PSQLException> { call, cause ->
            val (status, message) = mapSqlState(cause.sqlState, cause.message)
            call.respondApi<Unit>(status = status, message = message)
        }

        exception<SQLException> { call, cause ->
            val (status, message) = mapSqlState(cause.sqlState, cause.message)
            call.respondApi<Unit>(status = status, message = message)
        }

        // Catch-all
        exception<Throwable> { call, cause ->
            // Log unexpected errors for debugging/monitoring
            call.application.environment.log.error("Unhandled exception", cause)
            call.respondApi<Unit>(
                status = HttpStatusCode.InternalServerError,
                message = "Internal server error",
            )
        }
    }
}

private fun mapSqlState(sqlState: String?, rawMessage: String?): Pair<HttpStatusCode, String> {
    // Map common PostgreSQL SQLSTATE codes
    return when (sqlState) {
        "23505" -> HttpStatusCode.Conflict to ("Duplicate key / unique constraint violation") // unique_violation
        "23503" -> HttpStatusCode.Conflict to ("Foreign key constraint violation") // foreign_key_violation
        "23502" -> HttpStatusCode.BadRequest to ("Null value in column violates not-null constraint") // not_null_violation
        "23514" -> HttpStatusCode.BadRequest to ("Check constraint violation") // check_violation
        else -> {
            // Fallback: if message hints at unique constraint
            val msg = rawMessage ?: "Database error"
            if (msg.contains("unique", ignoreCase = true)) {
                HttpStatusCode.Conflict to "Unique constraint violation"
            } else {
                HttpStatusCode.InternalServerError to "Database error"
            }
        }
    }
}

class NotFoundException(message: String) : Exception(message)
class AuthenticationException(override val message: String?) : Exception(message)
class ForbiddenException(override val message: String?) : Exception(message)
class BadRequestException(message: String) : Exception(message)
class ConflictException(message: String) : Exception(message)
class ValidationException(message: String, val errors: List<String> = listOf(message)) : Exception(message) {
    constructor(errors: List<String>) : this(errors.joinToString(", "), errors)
}

open class PesapalException(message: String) : Exception(message)
class PesapalAuthException(message: String) : PesapalException(message)
