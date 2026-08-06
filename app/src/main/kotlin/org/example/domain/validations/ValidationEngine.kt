package org.example.domain.validations

import org.example.exceptions.ValidationException

class ValidationEngine(private val registry: ValidatorRegistry = DefaultValidationRegistry()) {
    fun <T: Any> validate(entity: T): Map<String, List<String>> {
        val errors = mutableMapOf<String, MutableList<String>>()
        val metadata = ClassMetadata.forClass(entity::class)

        metadata.constrainedProperties.forEach { constraint ->
            val value = constraint.property.getter.call(entity)
            if (value != null) {
                val fieldErrors = executeConstraint(constraint, value)
                if (fieldErrors.isNotEmpty()) {
                    errors.getOrPut(constraint.property.name) { mutableListOf() }
                        .addAll(fieldErrors)
                }
            }
        }
        return errors
    }

    fun <T: Any> validateAndThrow(entity: T): T {
        val errors = validate(entity)
        if (errors.isNotEmpty()) {
            val pluginErrors = errors.map { (field, errorList) ->
                ValidationError(field, errorList)
            }
            throw ValidationException(pluginErrors)
        }
        return entity
    }

    @Suppress("UNCHECKED_CAST")
    private fun executeConstraint(constraint: PropertyConstraint, value: Any): List<String> {
        return when (val factory = registry.getValidatorOrFactory(constraint.annotationClass)){
            is ValidatorFactory.Simple -> {
                val validator = factory.validator as FieldValidator<Any>
                validator.validate(value, constraint.property.name).errors
            }
            is ValidatorFactory.AnnotationAware -> {
                val validator = factory.factory(constraint.annotation) as ConstraintValidator<Annotation, Any>
                validator.validate(value, constraint.property.name).errors
            }
        }
    }
}