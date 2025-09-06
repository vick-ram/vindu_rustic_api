package org.example.plugins

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.plugins.statuspages.exception
import io.ktor.server.request.ContentTransformationException
import io.ktor.server.response.respond
import io.ktor.server.thymeleaf.ThymeleafContent
import org.attoparser.ParseException
import org.example.utils.respondApi
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.postgresql.util.PSQLException
import org.thymeleaf.exceptions.TemplateInputException
import org.thymeleaf.exceptions.TemplateProcessingException
import java.sql.SQLException

fun Application.configureStatusPages() {
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            cause.dynamicRespond(call)
        }
//        exception<AuthenticationException> { call, cause ->
//            call.respondApi<Unit>(
//                status = HttpStatusCode.Unauthorized,
//                message = cause.message ?: "Unauthorized",
//            )
//        }
//
//        exception<ForbiddenException> { call, cause ->
//            call.respondApi<Unit>(
//                status = HttpStatusCode.Forbidden,
//                message = cause.message ?: "Forbidden",
//            )
//        }
//
//        exception<NotFoundException> { call, cause ->
//            call.respondApi<Unit>(
//                status = HttpStatusCode.NotFound,
//                message = cause.message ?: "Not found",
//            )
//        }
//
//        exception<AlreadyExistsException> { call, cause ->
//            call.respondApi<Unit>(
//                status = HttpStatusCode.Conflict,
//                message = cause.message ?: "Already exits",
//            )
//        }
//
//        exception<NoSuchElementException> { call, cause ->
//            call.respondApi<Unit>(
//                status = HttpStatusCode.NotFound,
//                message = cause.message ?: "Not found",
//            )
//        }
//
//        exception<BadRequestException> { call, cause ->
//            call.respondApi<Unit>(
//                status = HttpStatusCode.BadRequest,
//                message = cause.message ?: "Bad Request",
//            )
//        }
//
//        exception<IllegalArgumentException> { call, cause ->
//            call.respondApi<Unit>(
//                status = HttpStatusCode.BadRequest,
//                message = cause.message ?: "Bad Request",
//            )
//        }
//
//        exception<NumberFormatException> { call, cause ->
//            call.respondApi<Unit>(
//                status = HttpStatusCode.BadRequest,
//                message = cause.message ?: "Invalid number format",
//            )
//        }
//
//        exception<ValidationException> { call, cause ->
//            call.respondApi<Unit>(
//                status = HttpStatusCode.BadRequest,
//                message = "Validation failed",
//                errors = cause.errors
//            )
//        }
//
//        exception<ContentTransformationException> { call, cause ->
//            call.application.environment.log.error("Unsupported media type", cause)
//            call.respondApi<Unit>(
//                status = HttpStatusCode.UnsupportedMediaType,
//                message = cause.message ?: "Unsupported media type",
//            )
//        }
//
//        exception<ConflictException> { call, cause ->
//            call.respondApi<Unit>(
//                status = HttpStatusCode.Conflict,
//                message = cause.message ?: "Conflict",
//            )
//        }
//
//        exception<ExposedSQLException> { call, cause ->
//            val state = (try {
//                cause.sqlState
//            } catch (_: Throwable) {
//                null
//            })
//                ?: (cause.cause as? SQLException)?.sqlState
//            val (status, message) = mapSqlState(state, cause.message)
//            call.respondApi<Unit>(status = status, message = message)
//        }
//
//        exception<PSQLException> { call, cause ->
//            val (status, message) = mapSqlState(cause.sqlState, cause.message)
//            call.respondApi<Unit>(status = status, message = message)
//        }
//
//        exception<SQLException> { call, cause ->
//            val (status, message) = mapSqlState(cause.sqlState, cause.message)
//            call.respondApi<Unit>(status = status, message = message)
//        }
//
//        exception<Throwable> { call, cause ->
//            call.respondApi<Unit>(
//                status = HttpStatusCode.InternalServerError,
//                message = "Internal server error",
//            )
//        }
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
class AlreadyExistsException(message: String): Exception(message)
class ConflictException(message: String) : Exception(message)
class ValidationException(message: String, val errors: List<String> = listOf(message)) : Exception(message) {
    constructor(errors: List<String>) : this(errors.joinToString(", "), errors)
}

open class PesapalException(message: String) : Exception(message)
class PesapalAuthException(message: String) : PesapalException(message)

suspend fun Throwable.dynamicRespond(call: ApplicationCall) {
    val accept = call.request.headers["Accept"] ?: ""

    val (status, message, errors) = when (this) {
        is AuthenticationException ->
            Triple(HttpStatusCode.Unauthorized, message ?: "Unauthorized", null)

        is ForbiddenException ->
            Triple(HttpStatusCode.Forbidden, message ?: "Forbidden", null)

        is NotFoundException, is NoSuchElementException ->
            Triple(HttpStatusCode.NotFound, message ?: "Not found", null)

        is BadRequestException, is IllegalArgumentException -> Triple(HttpStatusCode.BadRequest, message ?: "Bad Request", null)

        is ContentTransformationException -> Triple(HttpStatusCode.UnsupportedMediaType, message ?: "Unsupported media type", null)

        is AlreadyExistsException, is ConflictException ->
            Triple(HttpStatusCode.Conflict, message ?: "Already exists", null)

        is ValidationException ->
            Triple(HttpStatusCode.BadRequest, "Validation failed", this.errors)

        is ExposedSQLException -> {
            val state = (try {
                sqlState
            } catch (_: Throwable) {
                null
            })
                ?: (cause as? SQLException)?.sqlState
            mapSqlState(state, this.message).let { (s, m) -> Triple(s, m, null) }
        }

        is PSQLException ->
            mapSqlState(sqlState, this.message).let { (s, m) -> Triple(s, m, null) }

        is SQLException ->
            mapSqlState(sqlState, this.message).let { (s, m) -> Triple(s, m, null) }

        is TemplateInputException, is ParseException, is TemplateProcessingException -> Triple(HttpStatusCode.UnprocessableEntity, message ?: "template parsing error", null)

        else ->
            Triple(HttpStatusCode.InternalServerError, message ?: "Internal server error", null)
    }

    if (accept.contains("text/html")) {
        val model: MutableMap<String, Any> = mutableMapOf(
            "status" to status.value,
            "message" to message
        )

        if (errors != null) {
            model["errors"] = errors
        }
        val template = when(status) {
            HttpStatusCode.NotFound -> ThymeleafContent("error/404.html", model)
            else -> ThymeleafContent("error/500.html", model)
        }
        call.respond(
            status,
            template
        )
    } else {
        call.respondApi<Unit>(
            status = status,
            message = message,
            errors = errors
        )
    }
}


