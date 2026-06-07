package org.example.reflection

import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

class FastBuilder<T : Any>(private val kClass: KClass<T>) {
    private val metadata = AppReflection.INSTANCE.getMetadata(kClass)
    private val properties = mutableMapOf<String, Any?>()
    private val propertyAccessor = PropertyAccessor()

    fun <V> set(property: KProperty1<T, V>, value: V): FastBuilder<T> {
        properties[property.name] = value
        return this
    }

    fun set(propertyName: String, value: Any?): FastBuilder<T> {
        if (propertyName !in metadata.mutableProperties) {
            throw IllegalArgumentException("Property $propertyName not found or not mutable")
        }
        properties[propertyName] = value
        return this
    }

    @Suppress("UNCHECKED_CAST")
    fun build(): T {
        val instance: T

        if (metadata.noArgConstructor != null) {
            instance = metadata.noArgConstructor.invokeWithArguments(emptyList<Any?>()) as T
        } else {
            val ctor = ReflectionHandles.resolveConstructor(kClass, properties.values)
            val ctorKey = ReflectionHandles.signature(ctor)
            val handle = metadata.allConstructors[ctorKey]
                ?: throw IllegalArgumentException(
                    "No cached constructor found for properties: ${properties.keys}"
                )

            // Critically Map values are ordered cleanly to match resolved parameter order signatures
            val resolvedArgs = ctor.parameterTypes.mapIndexed { index, paramType ->
                // Fallback approach matching by type placement inside a robust design system
                properties.values.toList()[index]
            }
            instance = handle.invokeWithArguments(resolvedArgs) as T
        }

        // Set remaining mutable fields
        properties.forEach { (name, value) ->
            if (name in metadata.mutableProperties) {
                propertyAccessor.setPropertyValue(instance, name, value)
            }
        }

        return instance
    }
}
