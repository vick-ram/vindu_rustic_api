package org.example.domain.validations

import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

interface ValidatorRegistry {
    fun getValidatorOrFactory(annotationClass: KClass<*>): ValidatorFactory
    fun getRegisteredAnnotations(): Set<KClass<*>>
}

sealed class ValidatorFactory {
    data class Simple(val validator: FieldValidator<*>): ValidatorFactory()
    data class AnnotationAware(val factory: (Annotation) -> ConstraintValidator<*, *>): ValidatorFactory()
}

class DefaultValidationRegistry : ValidatorRegistry {

    private val factories = ConcurrentHashMap<KClass<*>, ValidatorFactory>()

    init {
        registerSimple(NotBlank::class, NotBlankValidator())
        registerSimple(Email::class, EmailValidator())
        registerSimple(Phone::class, PhoneValidator())
        registerSimple(FutureDate::class, FutureDateValidator())
        registerSimple(FutureDateTime::class, FutureDateTimeValidator())

        registerAnnotationAware(MinLength::class) {annotation ->
            MinLengthValidator().also { it.initialize(annotation)}
        }
        registerAnnotationAware(MaxLength::class) { annotation ->
            MaxLengthValidator().also { it.initialize(annotation)}
        }
        registerAnnotationAware(LessThan::class) { annotation ->
            LessThanValidator().also { it.initialize(annotation)}
        }
        registerAnnotationAware(GreaterThan::class) { annotation ->
            GreaterThanValidator().also { it.initialize(annotation)}
        }
        registerAnnotationAware(Password::class) { annotation ->
            PasswordValidator().also { it.initialize(annotation)}
        }
        registerAnnotationAware(ValidEnum::class) { annotation ->
            EnumValidator().also { it.initialize(annotation)}
        }
        registerAnnotationAware(DateFormat::class) { annotation ->
            DateFormatValidator().also { it.initialize(annotation)}
        }
        registerAnnotationAware(DateTimeFormat::class) { annotation ->
            DateTimeFormatValidator().also { it.initialize(annotation)}
        }
        registerAnnotationAware(Between::class) { annotation ->
            BetweenValidator().also { it.initialize(annotation)}
        }
    }

    private fun <A: Annotation, T> registerSimple(
        annotationClass: KClass<A>,
        validator: FieldValidator<T>
    ) {
        factories[annotationClass] = ValidatorFactory.Simple(validator)
    }

    @Suppress("UNCHECKED_CAST")
    private fun <A: Annotation, T> registerAnnotationAware(
        annotationClass: KClass<A>,
        factory: (A) -> ConstraintValidator<A, T>
    ) {
        factories[annotationClass] = ValidatorFactory.AnnotationAware(
            factory as (Annotation) -> ConstraintValidator<*, *>
        )
    }

    @Suppress("UNCHECKED_CAST")
    override fun getValidatorOrFactory(annotationClass: KClass<*>): ValidatorFactory {
        return factories[annotationClass]
            ?: throw IllegalArgumentException("No validator registered for ${annotationClass.simpleName}")
    }

    override fun getRegisteredAnnotations(): Set<KClass<*>> = factories.keys
}