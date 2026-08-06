package org.example.plugins

import io.ktor.http.*
import io.ktor.server.plugins.NotFoundException
import io.r2dbc.postgresql.api.PostgresqlException
import io.r2dbc.spi.R2dbcException
import org.example.exceptions.*
import org.thymeleaf.exceptions.TemplateInputException
import org.thymeleaf.exceptions.TemplateProcessingException
import java.sql.SQLException
import java.text.ParseException

class ExceptionMapper {
    fun map(exception: Throwable): Triple<HttpStatusCode, String, List<String>?> = when (exception) {
        is AuthenticationException ->
            Triple(HttpStatusCode.Unauthorized, exception.message ?: "Unauthorized", null)

        is ForbiddenException ->
            Triple(HttpStatusCode.Forbidden, exception.message ?: "Forbidden", null)

        is NotFoundException, is NoSuchElementException ->
            Triple(HttpStatusCode.NotFound, exception.message ?: "Not found", null)

        is AlreadyExistsException, is ConflictException, is DuplicateTransactionException ->
            Triple(HttpStatusCode.Conflict, exception.message ?: "Conflict", null)

        is BadRequestException, is IllegalArgumentException ->
            Triple(HttpStatusCode.BadRequest, exception.message ?: "Bad Request", null)

        is ValidationException -> {
            val errorMessages = exception.fieldErrors.flatMap { ve ->
                ve.errors.map { "${ve.field}: $it" }
            }
            Triple(HttpStatusCode.UnprocessableEntity, "Validation failed", errorMessages)
        }

        is InsufficientInventoryException ->
            Triple(
                HttpStatusCode.Conflict,
                exception.message!!,
                listOf(
                    "variantId=${exception.variantId}",
                    "required=${exception.required}",
                    "available=${exception.available}"
                )
            )

        is InvalidCouponException ->
            Triple(HttpStatusCode.BadRequest, exception.message ?: "Invalid coupon", null)

        is EmptyCartException ->
            Triple(HttpStatusCode.BadRequest, exception.message ?: "Cart is empty", null)

        is TwoFactorRequiredException ->
            Triple(HttpStatusCode.Forbidden, exception.message!!, listOf("userId=${exception.userId}"))

        is OtpExpiredException ->
            Triple(HttpStatusCode.BadRequest, exception.message!!, null)

        is InvalidOtpException ->
            Triple(
                HttpStatusCode.BadRequest,
                exception.message!!,
                listOf("remainingAttempts=${exception.remainingAttempts}")
            )

        is TooManyAttemptsException, is TokenExpiredException ->
            Triple(HttpStatusCode.TooManyRequests, exception.message!!, null)

        is RouteValidationException ->
            Triple(HttpStatusCode.BadRequest, exception.message ?: "Route validation failed", null)

        is RouteConflictException ->
            Triple(HttpStatusCode.Conflict, exception.message ?: "Route conflict", null)

        is FileStorageException ->
            Triple(HttpStatusCode.InternalServerError, exception.message ?: "File storage error", null)

        is R2dbcException, is SQLException -> mapDatabaseException(exception)

        is TemplateInputException, is ParseException, is TemplateProcessingException ->
            Triple(HttpStatusCode.UnprocessableEntity, exception.message ?: "Template parsing error", null)

        else -> Triple(HttpStatusCode.InternalServerError, exception.message ?: "Internal server error", null)
    }

    private fun mapDatabaseException(exception: Exception): Triple<HttpStatusCode, String, List<String>?> {
        val sqlState = when (exception) {
            is PostgresqlException -> exception.errorDetails.code
            is R2dbcException -> exception.sqlState
            is SQLException -> exception.sqlState
            else -> null
        }

        val message = exception.message ?: "Database error"

        return when (sqlState) {
            "23505" -> Triple(HttpStatusCode.Conflict, "Duplicate key / unique constraint violation", null)
            "23503" -> Triple(HttpStatusCode.Conflict, "Foreign key constraint violation", null)
            "23502" -> Triple(HttpStatusCode.BadRequest, "Null value in column violates not-null constraint", null)
            "23514" -> Triple(HttpStatusCode.BadRequest, "Check constraint violation", null)
            else -> {
                if (message.contains("unique", ignoreCase = true)) {
                    Triple(HttpStatusCode.Conflict, "Unique constraint violation", null)
                } else {
                    Triple(HttpStatusCode.InternalServerError, "Database error", null)
                }
            }
        }
    }
}