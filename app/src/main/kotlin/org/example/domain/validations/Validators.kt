package org.example.domain.validations

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.FormatStringsInDatetimeFormats
import kotlinx.datetime.format.byUnicodePattern
import kotlinx.datetime.toLocalDateTime
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import kotlin.reflect.KClass
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class NotBlankValidator : FieldValidator<String> {
    override fun validate(
        value: String,
        fieldName: String
    ): ValidationResult {
        return if (value.isBlank()) {
            ValidationResult.failure("$fieldName cannot be blank")
        } else ValidationResult.success()
    }
}

class MinLengthValidator : ConstraintValidator<MinLength, String> {
    private var minLength: Int = 0
    private var customMessage: String = ""

    override fun initialize(annotation: MinLength) {
        this.minLength = annotation.value
        this.customMessage = annotation.message
    }

    override fun validate(
        value: String,
        fieldName: String
    ): ValidationResult {
        val message = customMessage.replace("{value}", minLength.toString())
        return if (value.length < minLength) {
            ValidationResult.failure(message)
        } else ValidationResult.success()
    }

    fun validate(value: String, min: Int, fieldName: String): ValidationResult {
        return if (value.length < min) {
            ValidationResult.failure("$fieldName should be at least $min characters")
        } else ValidationResult.success()
    }
}

class MaxLengthValidator : ConstraintValidator<MaxLength ,String> {
    private var maxLength: Int = 0
    private var customMessage: String = ""

    override fun initialize(annotation: MaxLength) {
        this.maxLength = annotation.value
        this.customMessage = annotation.message
    }

    override fun validate(
        value: String,
        fieldName: String
    ): ValidationResult {
        val message = customMessage.replace("{value}", maxLength.toString())
        return if (value.length > maxLength) {
            ValidationResult.failure(message)
        } else ValidationResult.success()
    }
}

class LessThanValidator : ConstraintValidator<LessThan, Int> {
    private var maxValue: Int = 0
    private var customMessage: String = ""

    override fun initialize(annotation: LessThan) {
        this.maxValue = annotation.value
        this.customMessage = annotation.message
    }

    override fun validate(
        value: Int,
        fieldName: String
    ): ValidationResult {
        val message = customMessage.replace("{value}", maxValue.toString())
        return if (value >= maxValue ) {
            ValidationResult.failure(message)
        } else ValidationResult.success()
    }
}

class GreaterThanValidator : ConstraintValidator<GreaterThan, Int> {
    private var minValue: Int = 0
    private var customMessage: String = ""

    override fun initialize(annotation: GreaterThan) {
        this.minValue = annotation.value
        this.customMessage = annotation.message
    }

    override fun validate(
        value: Int,
        fieldName: String
    ): ValidationResult {
        val message = customMessage.replace("{value}", minValue.toString())
        return if (value <= minValue ) {
            ValidationResult.failure(message)
        } else ValidationResult.success()
    }
}

class EmailValidator : FieldValidator<String> {
    private val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")

    override fun validate(
        value: String,
        fieldName: String
    ): ValidationResult {
        val trimmed = value.trim()
        return when {
            trimmed.isBlank() -> ValidationResult.failure("$fieldName cannot be blank")
            !trimmed.matches(emailRegex) -> ValidationResult.failure("$fieldName must be valid format")
            else -> ValidationResult.success()
        }
    }
}

class PasswordValidator : ConstraintValidator<Password ,String> {
    private var minLength: Int = 8
    private var requireUppercase: Boolean = true
    private var requireLowercase: Boolean = true
    private var requireDigit: Boolean = true
    private var requireSpecialChar: Boolean = true

    override fun initialize(annotation: Password) {
        this.minLength = annotation.minLength
        this.requireUppercase = annotation.requireUppercase
        this.requireLowercase = annotation.requireLowercase
        this.requireDigit = annotation.requireDigit
        this.requireSpecialChar = annotation.requireSpecialChar
    }

    override fun validate(
        value: String,
        fieldName: String
    ): ValidationResult {
        val errors = mutableListOf<String>()
        val trimmed = value.trim()

        if (trimmed.isBlank()) {
            errors.add("$fieldName cannot be blank")
        } else {
            if (trimmed.length < minLength) {
                errors.add("$fieldName should be at least $minLength characters")
            }
            if (requireUppercase && !trimmed.any(Char::isUpperCase)) {
                errors.add("$fieldName must contain at least one uppercase character")
            }
            if (requireLowercase && !trimmed.any(Char::isLowerCase)) {
                errors.add("$fieldName must contain at least one lowercase character")
            }
            if (requireDigit && !trimmed.any(Char::isDigit)) {
                errors.add("$fieldName must contain at least one digit character")
            }
            if (requireSpecialChar && !trimmed.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*".toRegex())) {
                errors.add("$fieldName must contain at least one special char")
            }
        }
        return if (errors.isEmpty()) ValidationResult.success() else ValidationResult.failure(errors)
    }
}

class EnumValidator: ConstraintValidator<ValidEnum ,Enum<*>> {
    private var enumClass: KClass<out Enum<*>>? = null

    override fun initialize(annotation: ValidEnum) {
        this.enumClass = annotation.enumClass
    }

    override fun validate(
        value: Enum<*>,
        fieldName: String
    ): ValidationResult {
        val kClass = enumClass ?: return ValidationResult.failure("Enum class not configured")

        return try {
            val enumConstants = kClass.java.enumConstants
            val validNames = enumConstants.map { it.name }

            if (value.name in validNames) {
                ValidationResult.success()
            } else {
                ValidationResult.failure(
                    "Invalid $fieldName. Valid values are: ${validNames.joinToString()}"
                )
            }
        } catch (_: Exception) {
            ValidationResult.failure("Invalid enum class for field $fieldName")
        }
    }

    fun validate(value: Enum<*>, enumClass: KClass<out Enum<*>>, fieldName: String): ValidationResult {
        return try {
            val enumConstants = enumClass.java.enumConstants
            val validNames = enumConstants.map { it.name }

            if (value.name in validNames) {
                ValidationResult.success()
            } else {
                ValidationResult.failure(
                    "Invalid $fieldName. Valid values are: ${validNames.joinToString()}"
                )
            }
        } catch (_: Exception) {
            ValidationResult.failure("Invalid enum class for field $fieldName")
        }
    }
}

class PhoneValidator : FieldValidator<String> {
    override fun validate(
        value: String,
        fieldName: String
    ): ValidationResult {
        val trimmed = value.trim()
        val errors = mutableListOf<String>()

        if (trimmed.isBlank()) {
            errors.add("Phone number cannot be blank")
        } else {
            if (!trimmed.startsWith("07") && !trimmed.startsWith("01")) {
                errors.add("Phone number must start with either 07 or 01")
            }
            if (!trimmed.matches("^\\d{10}$".toRegex())) {
                errors.add("Phone must be exactly 10 digits")
            }
        }
        return if (errors.isEmpty()) ValidationResult.success() else ValidationResult.failure(errors)
    }
}

class FutureDateValidator : FieldValidator<LocalDate> {
    @OptIn(ExperimentalTime::class)
    override fun validate(
        value: LocalDate,
        fieldName: String
    ): ValidationResult {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        return if (value < today) {
            ValidationResult.failure("$fieldName cannot be in the past")
        } else ValidationResult.success()
    }
}

class FutureDateTimeValidator : FieldValidator<OffsetDateTime> {
    @OptIn(ExperimentalTime::class)
    override fun validate(
        value: OffsetDateTime,
        fieldName: String
    ): ValidationResult {
        val now = OffsetDateTime.now()
        return if (value < now) {
            ValidationResult.failure("$fieldName cannot be in the past")
        } else ValidationResult.success()
    }
}

class DateFormatValidator : ConstraintValidator<DateFormat, LocalDate> {
    private var pattern: String = "yyyy-MM-dd"
    private var customMessage: String = ""

    override fun initialize(annotation: DateFormat) {
        this.pattern = annotation.pattern
        this.customMessage = annotation.message
    }

    @OptIn(FormatStringsInDatetimeFormats::class)
    override fun validate(
        value: LocalDate,
        fieldName: String
    ): ValidationResult {
        val message =customMessage.replace("{pattern}", pattern)
        val dateStr = value.toString()

        if (dateStr.isBlank()) {
            return ValidationResult.failure("$fieldName cannot be blank")
        }

        val formatter = LocalDate.Format { byUnicodePattern(pattern) }
        val parsed = runCatching { LocalDate.parse(dateStr, formatter) }.getOrNull()

        return if (parsed == null) {
            ValidationResult.failure(message)
        } else ValidationResult.success()
    }
}

class DateTimeFormatValidator : ConstraintValidator<DateTimeFormat, OffsetDateTime> {
    private var pattern: String = "yyyy-MM-dd HH:mm"
    private var customMessage: String = ""

    override fun initialize(annotation: DateTimeFormat) {
        this.pattern = annotation.pattern
        this.customMessage = annotation.message
    }

    @OptIn(FormatStringsInDatetimeFormats::class)
    override fun validate(
        value: OffsetDateTime,
        fieldName: String
    ): ValidationResult {
        val message =customMessage.replace("{pattern}", pattern)
        val dateTimeStr = value.toString()

        if (dateTimeStr.isBlank()) {
            return ValidationResult.failure("$fieldName cannot be blank")
        }

//        val formatter = OffsetDateTime.Format { byUnicodePattern(pattern) }
        val formatter = DateTimeFormatter.ofPattern(pattern)
        val parsed = runCatching { OffsetDateTime.parse(dateTimeStr, formatter) }.getOrNull()

        return if (parsed == null) {
            ValidationResult.failure(message)
        } else ValidationResult.success()
    }
}

class BetweenValidator : ConstraintValidator<Between, Int> {
    private var min: Int = 0
    private var max: Int = 0
    private var customMessage: String = ""

    override fun initialize(annotation: Between) {
        this.min = annotation.min
        this.max = annotation.max
        this.customMessage = annotation.message
    }

    override fun validate(
        value: Int,
        fieldName: String
    ): ValidationResult {
        val message = customMessage.replace("{min}", min.toString()).replace("{max}", max.toString())
        return if (value !in min..max) {
            ValidationResult.failure(message)
        } else ValidationResult.success()
    }
}