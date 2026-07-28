package org.example.plugins

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.plugins.statuspages.exception
import io.ktor.server.request.ContentTransformationException
import io.ktor.server.request.path
import io.ktor.server.response.respond
import io.ktor.server.thymeleaf.ThymeleafContent
import io.ktor.util.AttributeKey
import org.attoparser.ParseException
import org.example.config.ApplicationPlugin
import org.example.utils.respondApi
import org.example.utils.respondHtml
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.postgresql.util.PSQLException
import org.thymeleaf.exceptions.TemplateInputException
import org.thymeleaf.exceptions.TemplateProcessingException
import java.sql.SQLException
import java.time.OffsetDateTime

object StatusPagesModule : ApplicationPlugin {
    override fun install(application: Application) {
        application.install(StatusPages) {
            exception<Throwable> { call, cause ->
                cause.dynamicRespond(call)
            }
        }
    }
}

class TemplateContext(var currentTemplate: String? = null)

val TemplateContextKey = AttributeKey<TemplateContext>("TemplateContext")

fun Application.installTemplateContext() {
    intercept(ApplicationCallPipeline.Setup) {
        call.attributes.put(TemplateContextKey, TemplateContext())
        proceed()
    }
}

fun ApplicationCall.setCurrentTemplate(template: String) {
    attributes[TemplateContextKey].currentTemplate = template
}

fun ApplicationCall.getCurrentTemplate(): String? {
    return attributes.getOrNull(TemplateContextKey)?.currentTemplate
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
class AlreadyExistsException(message: String) : Exception(message)
class ConflictException(message: String) : Exception(message)
class TokenExpiredException(message: String, expiresAt: OffsetDateTime) : Exception(message)
class TwoFactorRequiredException(val userId: String) : RuntimeException("2FA verification required")
class OtpExpiredException : RuntimeException("OTP has expired")
class InvalidOtpException(val remainingAttempts: Int) : RuntimeException("Invalid OTP. $remainingAttempts attempts remaining")
class TooManyAttemptsException : RuntimeException("Too many attempts. Please request a new OTP")
class ValidationException(
    val fieldErrors: List<ValidationError>
) : Exception() {
    constructor(field: String, error: String): this(
        listOf(ValidationError(field, listOf(error)))
    )

    constructor(field: String, errors: List<String>): this(
        listOf(ValidationError(field, errors))
    )
}

class RouteValidationException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
class RouteConflictException(message: String) : RuntimeException(message)
class FileStorageException(message: String, cause: Throwable) : Exception(message, cause)
class InsufficientInventoryException(
    val variantId: String,
    val required: Int,
    val available: Int,
    cause: Throwable? = null
) : RuntimeException(
    "Insufficient inventory for variant $variantId: requested $required, available $available",
    cause
)

class InvalidCouponException(message: String) : RuntimeException(message)
class EmptyCartException(cartId: String) : RuntimeException("Cart is empty: $cartId")
class DuplicateTransactionException(message: String) : RuntimeException(message)
suspend fun Throwable.dynamicRespond(call: ApplicationCall) {
    val accept = call.request.headers["Accept"] ?: ""
    val currentTemplate = call.getCurrentTemplate()
    val requestPath = call.request.path()

    val (status, message, errors) = when (this) {
        is AuthenticationException ->
            Triple(HttpStatusCode.Unauthorized, message ?: "Unauthorized", null)

        is ForbiddenException ->
            Triple(HttpStatusCode.Forbidden, message ?: "Forbidden", null)

        is NotFoundException, is NoSuchElementException ->
            Triple(HttpStatusCode.NotFound, message ?: "Not found", null)

        is BadRequestException, is IllegalArgumentException -> Triple(
            HttpStatusCode.BadRequest,
            message ?: "Bad Request",
            null
        )

        is ContentTransformationException -> Triple(
            HttpStatusCode.UnsupportedMediaType,
            message ?: "Unsupported media type",
            null
        )

        is AlreadyExistsException, is ConflictException ->
            Triple(HttpStatusCode.Conflict, message ?: "Already exists", null)

        is ValidationException ->{
            val errorMessages = fieldErrors.flatMap { validationError ->
                validationError.errors.map { "${validationError.field}: $it" }
            }
            Triple(HttpStatusCode.UnprocessableEntity, "Validation failed", errorMessages)
        }

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

        is TemplateInputException, is ParseException, is TemplateProcessingException -> Triple(
            HttpStatusCode.UnprocessableEntity,
            message ?: "template parsing error",
            null
        )

        else ->
            Triple(HttpStatusCode.InternalServerError, message ?: "Internal server error", null)
    }

    val useCurrentTemplate = when {
        // Always use current template for these status codes on form pages
        status == HttpStatusCode.BadRequest && isFormPage(currentTemplate, requestPath) -> true
        status == HttpStatusCode.Conflict && isFormPage(currentTemplate, requestPath) -> true
        // Use current template if we're on a page that makes sense for the error
        currentTemplate != null && shouldUseCurrentTemplate(status, currentTemplate, requestPath) -> true
        else -> false
    }

    if (accept.contains("text/html") && useCurrentTemplate) {
        call.respondHtml<Unit>(
            template = currentTemplate!!,
            status = status,
            message = message,
            errors = errors
        )
    } else if (accept.contains("text/html")) {
        // Fallback to dedicated error pages
        val model: MutableMap<String, Any> = mutableMapOf(
            "status" to status.value,
            "message" to message
        )

        if (errors != null) {
            model["errors"] = errors
        }
        val template = when (status) {
            HttpStatusCode.NotFound -> "error/404.html"
            HttpStatusCode.Unauthorized -> "auth/login.html" // Redirect to login for auth errors
            else -> "error/500.html"
        }
        call.respond(status, ThymeleafContent(template, model))
    } else {
        call.respondApi<Unit>(
            status = status,
            message = message,
            errors = errors
        )
    }
}

private fun isFormPage(template: String?, path: String): Boolean {
    val formTemplates = setOf(
        "admin/pages/dashboard",
        "admin/pages/users/index",
        "admin/pages/users/detail",
        "admin/pages/products/categories",
        "admin/pages/products/new",
        "admin/pages/products/list",
        "admin/pages/products/discount",
        "admin/pages/products/reviews",
        "admin/pages/products/offer",
        "admin/pages/orders/index",
        "admin/pages/analytics",
        "admin/pages/settings",
        "admin/pages/help",
        "admin/pages/profile",
        "auth/signin.html",
        "auth/signup.html",
        "auth/forgot-password.html"
    )
    val formPaths = setOf(
        "/admin/dashboard",
        "/admin/users",
        "/admin/users/detail/{id}",
        "/admin/products/categories",
        "/admin/products/new",
        "/admin/products/list",
        "/admin/products/discount",
        "/admin/products/reviews",
        "/admin/products/offer",
        "/admin/orders",
        "/admin/analytics",
        "/admin/settings",
        "/admin/help",
        "/admin/profile",
        "/signin",
        "/signup",
        "/forgot-password",
        "/"
    )

    return template in formTemplates || path in formPaths
}

private fun shouldUseCurrentTemplate(status: HttpStatusCode, template: String, path: String): Boolean {
    return when (status) {
        HttpStatusCode.BadRequest, HttpStatusCode.Conflict, HttpStatusCode.Unauthorized ->
            isFormPage(template, path)

        else -> false
    }
}

data class ValidationError(
    val field: String,
    val errors: List<String>
)

