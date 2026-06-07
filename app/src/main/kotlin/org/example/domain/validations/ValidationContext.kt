package org.example.domain.validations

import org.example.plugins.ValidationError
import org.example.plugins.ValidationException

class ValidationContext<T: Any>(private val entity: T) {
    private val engine = ValidationEngine()
    private val customRules = mutableListOf<() -> Unit>()
    private val errorMap = mutableMapOf<String, MutableList<String>>()

    fun validateAnnotations(): ValidationContext<T> {
        val annotationErrors = engine.validate(entity)
        annotationErrors.forEach { (field, errors) ->
            errorMap.getOrPut(field) { mutableListOf() }.addAll(errors)
        }
        return this
    }

    fun rule(fieldName: String, validation: () -> ValidationResult): ValidationContext<T> {
        customRules.add {
            val result = validation()
            if (!result.isValid) {
                errorMap.getOrPut(fieldName) { mutableListOf() }.addAll(result.errors)
            }
        }
        return this
    }

    fun entityRule(validation: (T) -> ValidationResult): ValidationContext<T> {
        customRules.add {
            val result = validation(entity)
            if (!result.isValid) {
                errorMap.getOrPut("_entity") { mutableListOf() }.addAll(result.errors)
            }
        }
        return this
    }

    fun execute(): T {
        customRules.forEach { it.invoke() }

        if (errorMap.isNotEmpty()) {
            val errors = errorMap.map { (field, errorList) ->
                ValidationError(field, errorList)
            }
            throw ValidationException(errors)
        }
        return entity
    }

    fun getErrors(): Map<String, List<String>> {
        customRules.forEach { it.invoke() }
        return errorMap.toMap()
    }

    fun hasErrors(): Boolean {
        customRules.forEach { it.invoke() }
        return errorMap.isNotEmpty()
    }
}

fun <T: Any> T.validate(): T {
    return ValidationContext(this).validateAnnotations().execute()
}

