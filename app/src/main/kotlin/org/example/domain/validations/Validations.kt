package org.example.domain.validations

import org.example.plugins.ValidationException

object Validations {
    private val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$".toRegex()

    fun validateEmail(email: String): String {
        val emailErrors = mutableListOf<String>()

        if (email.isBlank()) emailErrors.add("Email cannot be blank")
        if (!email.matches(emailRegex)) emailErrors.add("Invalid email format")

        if (emailErrors.isNotEmpty()) throw ValidationException(emailErrors)

        return email.trim()
    }

    fun validatePassword(password: String, minLength: Int = 8): String {
        val errors = mutableListOf<String>()

        if (password.isBlank()) errors.add("Password cannot be blank")
        if (password.length < minLength) errors.add("Password must be at least $minLength characters")
        if (!password.any(Char::isUpperCase)) errors.add("Password must contain at least one uppercase letter")
        if (!password.any(Char::isLowerCase)) errors.add("Password must contain at least one lowercase letter")
        if (!password.any(Char::isDigit)) errors.add("Password must contain at least one digit")
        if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*".toRegex())) {
            errors.add("Password must contain at least one special character")
        }

        if (errors.isNotEmpty()) throw ValidationException(errors)
        return password
    }

    fun validateNonEmpty(value: String, fieldName: String): String {
        if (value.isBlank()) throw ValidationException("$fieldName cannot be blank")
        return value.trim()
    }

    fun validateMinLength(value: String, min: Int, fieldName: String): String {
        validateNonEmpty(value, fieldName)
        if (value.length < min) throw ValidationException("$fieldName must be at least $min characters")
        return value
    }

    fun validateMaxLength(value: String, max: Int, fieldName: String): String {
        if (value.length > max) throw ValidationException("$fieldName cannot exceed $max characters")
        return value
    }

    fun validateLessThan(value: Int, max: Int, fieldName: String): Int {
        if (value >= max) throw ValidationException("$fieldName must be less than $max")
        return value
    }

    fun validateGreaterThan(value: Int, min: Int, fieldName: String): Int {
        if (value <= min) throw ValidationException("$fieldName must be greater than $min")
        return value
    }
}