package org.example.domain.validations

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.FormatStringsInDatetimeFormats
import kotlinx.datetime.format.byUnicodePattern
import kotlinx.datetime.toLocalDateTime
import org.example.plugins.ValidationException
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

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
        val errors = mutableListOf<String>()

        if (value.isBlank()) errors.add("$fieldName cannot be blank")

        if (value.length < min) errors.add("$fieldName must be at least $min characters")
        if (errors.isNotEmpty()) throw ValidationException(errors)
        return value
    }

    fun validateMaxLength(value: String, max: Int, fieldName: String): String {
        val errors = mutableListOf<String>()

        if (value.isBlank()) errors.add("$fieldName cannot be blank")
        if (value.length > max) errors.add("$fieldName cannot exceed $max characters")

        if (errors.isNotEmpty()) throw ValidationException(errors)
        return value
    }

    fun validateLessThan(value: Int, max: Int, fieldName: String): Int {
        val errors = mutableListOf<String>()

        if (value.toString().isBlank()) errors.add("$fieldName cannot be blank")
        if (value >= max) errors.add("$fieldName must be less than $max")

        if (errors.isNotEmpty()) throw ValidationException(errors)
        return value
    }

    fun validateGreaterThan(value: Int, min: Int, fieldName: String): Int {
        val errors = mutableListOf<String>()

        if (value.toString().isBlank()) errors.add("$fieldName cannot be blank")

        if (value <= min) errors.add("$fieldName must be greater than $min")

        if (errors.isNotEmpty()) throw ValidationException(errors)
        return value
    }

    inline fun <reified T : Enum<T>> validateEnum(
        value: String,
        fieldName: String
    ): String {
        val errors = mutableListOf<String>()

        if (value.isBlank()) {
            errors.add("$fieldName cannot be blank")
        } else {
            try {
                enumValueOf<T>(value.uppercase())
            } catch (e: IllegalArgumentException) {
                val validValues = enumValues<T>().joinToString { it.name }
                errors.add("Invalid $fieldName. Valid values are: $validValues")
            }
        }

        if (errors.isNotEmpty()) throw ValidationException(errors)
        return value
    }


    @OptIn(FormatStringsInDatetimeFormats::class)
    fun validateDate(
        dateStr: String,
        fieldName: String,
        pattern: String = "yyyy-MM-dd"
    ): LocalDate {
        val errors = mutableListOf<String>()

        if (dateStr.isBlank()) errors.add("$fieldName cannot be blank")

        val formatter = LocalDate.Format {
            byUnicodePattern(pattern)
        }

        val parsed = runCatching { LocalDate.parse(dateStr, formatter) }.getOrNull()

        if (parsed == null) errors.add("Invalid $fieldName format. Expected $pattern")

        if (errors.isNotEmpty()) throw ValidationException(errors)

        return parsed.let { it as LocalDate }
    }

    @OptIn(FormatStringsInDatetimeFormats::class)
    fun validateDateTime(
        dateTimeStr: String,
        fieldName: String,
        pattern: String = "yyyy-MM-dd HH:mm"
    ): LocalDateTime {
        val errors = mutableListOf<String>()

        if (dateTimeStr.isBlank()) errors.add("$fieldName cannot be blank")

        val formatter = LocalDateTime.Format {
            byUnicodePattern(pattern)
        }

        val parsed = runCatching { LocalDateTime.parse(dateTimeStr, formatter) }.getOrNull()

        if (parsed == null) errors.add("Invalid $fieldName format. Expected $pattern")

        return parsed.let { it as LocalDateTime }
    }

    /**
     * Ensure a date is not in the past.
     */
    @OptIn(ExperimentalTime::class)
    fun validateFutureDate(value: LocalDate, fieldName: String): LocalDate {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        if (value < today) throw ValidationException("$fieldName cannot be in the past")
        return value
    }

    /**
     * Ensure a datetime is not in the past.
     */
    @OptIn(ExperimentalTime::class)
    fun validateFutureDateTime(value: LocalDateTime, fieldName: String): LocalDateTime {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        if (value < now) throw ValidationException("$fieldName cannot be in the past")
        return value
    }

    fun validateAll(vararg validations: () -> Unit) {
        val errors = mutableListOf<String>()

        validations.forEach { validation ->
            try {
                validation()
            } catch (e: ValidationException) {
                errors.addAll(e.errors)
            }
        }

        if (errors.isNotEmpty()) {
            throw ValidationException(errors)
        }
    }
}
