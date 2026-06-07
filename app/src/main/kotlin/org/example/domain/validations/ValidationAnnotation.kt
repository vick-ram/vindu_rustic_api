package org.example.domain.validations

import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class ConstraintAnnotation


@ConstraintAnnotation
@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class NotBlank(val message: String = "cannot be blank")

@ConstraintAnnotation
@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class MinLength(val value: Int, val message: String = "must be at least {value} characters")

@ConstraintAnnotation
@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class MaxLength(val value: Int, val message: String = "must not exceed {value} characters")

@ConstraintAnnotation
@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class LessThan(val value: Int, val message: String = "must be less than {value}")

@ConstraintAnnotation
@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class GreaterThan(val value: Int, val message: String = "must be greater than {value}")

@ConstraintAnnotation
@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class Email(val message: String = "must be a valid email address")

@ConstraintAnnotation
@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class Password(
    val minLength: Int = 8,
    val requireUppercase: Boolean = true,
    val requireLowercase: Boolean = true,
    val requireDigit: Boolean = true,
    val requireSpecialChar: Boolean = true
)

@ConstraintAnnotation
@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class Phone(val message: String = "must be a valid phone number")

@ConstraintAnnotation
@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class DateFormat(
    val pattern: String = "yyyy-MM-dd",
    val message: String = "must be a valid date in format {pattern}"
)

@ConstraintAnnotation
@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class DateTimeFormat(
    val pattern: String = "yyyy-MM-dd HH:mm",
    val message: String = "must be a valid datetime in format {pattern}"
)

@ConstraintAnnotation
@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class FutureDate(val message: String = "must be in the future")

@ConstraintAnnotation
@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class FutureDateTime(val message: String = "must be in the future")

@ConstraintAnnotation
@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class ValidEnum(
    val enumClass: KClass<out Enum<*>>,
    val message: String = "must be one of the valid values"
)

@ConstraintAnnotation
@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class Between(val min: Int, val max: Int, val message: String = "must be between {min} and {max} inclusive")

@ConstraintAnnotation
@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class CustomValidation(
    val validatorClass: KClass<out FieldValidator<*>>
)
