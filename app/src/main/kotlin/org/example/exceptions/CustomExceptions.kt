package org.example.exceptions

import org.example.domain.validations.ValidationError
import java.time.OffsetDateTime

// Group related exceptions
sealed class ApplicationException(message: String, cause: Throwable? = null) : Exception(message, cause)

// Authentication & Authorization
class AuthenticationException(override val message: String?) : ApplicationException(message ?: "Unauthorized")
class ForbiddenException(override val message: String?) : ApplicationException(message ?: "Forbidden")
class TokenExpiredException(message: String, val expiresAt: OffsetDateTime) : ApplicationException(message)

// Resource Exceptions
class NotFoundException(message: String) : ApplicationException(message)
class AlreadyExistsException(message: String) : ApplicationException(message)
class ConflictException(message: String) : ApplicationException(message)

// Validation Exceptions
class BadRequestException(message: String) : ApplicationException(message)
class ValidationException(val fieldErrors: List<ValidationError>) : ApplicationException("Validation failed") {
    constructor(field: String, error: String) : this(listOf(ValidationError(field, listOf(error))))
    constructor(field: String, errors: List<String>) : this(listOf(ValidationError(field, errors)))
}

// Business Logic Exceptions
class InsufficientInventoryException(
    val variantId: String,
    val required: Int,
    val available: Int,
    cause: Throwable? = null
) : ApplicationException("Insufficient inventory for variant $variantId: requested $required, available $available", cause)

class InvalidCouponException(message: String) : ApplicationException(message)
class EmptyCartException(cartId: String) : ApplicationException("Cart is empty: $cartId")
class DuplicateTransactionException(message: String) : ApplicationException(message)

// OTP Exceptions
class TwoFactorRequiredException(val userId: String) : ApplicationException("2FA verification required")
class OtpExpiredException : ApplicationException("OTP has expired")
class InvalidOtpException(val remainingAttempts: Int) : ApplicationException("Invalid OTP. $remainingAttempts attempts remaining")
class TooManyAttemptsException : ApplicationException("Too many attempts. Please request a new OTP")

// Infrastructure Exceptions
class RouteValidationException(message: String, cause: Throwable? = null) : ApplicationException(message, cause)
class RouteConflictException(message: String) : ApplicationException(message)
class FileStorageException(message: String, cause: Throwable) : ApplicationException(message, cause)
