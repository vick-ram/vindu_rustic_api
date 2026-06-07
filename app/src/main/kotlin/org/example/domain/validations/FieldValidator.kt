package org.example.domain.validations

interface ConstraintValidator<A : Annotation, T> {
    fun initialize(annotation: A)
    fun validate(value: T, fieldName: String): ValidationResult
}

interface FieldValidator<T> {
    fun validate(value: T, fieldName: String): ValidationResult
}

data class ValidationResult(
    val isValid: Boolean,
    val errors: List<String> = emptyList()
) {
    companion object {
        fun success() = ValidationResult(true)
        fun failure(errors: List<String>) = ValidationResult(false, errors)
        fun failure(error: String) = ValidationResult(false, listOf(error))
    }
}
