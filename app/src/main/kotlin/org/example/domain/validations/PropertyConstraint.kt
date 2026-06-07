package org.example.domain.validations

import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties

data class PropertyConstraint(
    val property: KProperty1<*, *>,
    val annotation: Annotation,
    val annotationClass: KClass<*>,
)

class ClassMetadata<T: Any> private constructor(
    val clazz: KClass<*>,
    val constrainedProperties: List<PropertyConstraint>
) {
    companion object {
        private val cache = ConcurrentHashMap<KClass<*>, ClassMetadata<*>>()

        @Suppress("UNCHECKED_CAST")
        fun <T: Any> forClass(clazz: KClass<T>): ClassMetadata<T> {
            return cache.getOrPut(clazz) {
                val properties = clazz.memberProperties
                    .flatMap { property ->
                        property.annotations
                            .filterIsInstance<ConstraintAnnotation>()
                            .map { annotation ->
                                PropertyConstraint(
                                    property = property,
                                    annotation = annotation,
                                    annotationClass = annotation.annotationClass
                                )
                            }
                    }

                ClassMetadata<T>(clazz, properties)
            } as ClassMetadata<T>
        }

        fun clearCache() = cache.clear()
    }
}
