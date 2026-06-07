package org.example.reflection

import java.util.function.BiConsumer
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty1
import kotlin.reflect.jvm.javaGetter
import kotlin.reflect.jvm.javaSetter
import java.util.function.Function

class PropertyAccessor {
    @PublishedApi
    internal val appReflection = AppReflection.INSTANCE

    /**
     * Creates a Java Function getter (not Kotlin Function1)
     * backed by a cached MethodHandle adapter.
     */
    inline fun <reified T, reified R> createPropertyGetter(
        property: KProperty1<T, R>
    ): Function<T, R> {
        val metadata = appReflection.getMetadata(T::class)

        @Suppress("UNCHECKED_CAST")
        return metadata.getterLambdas.computeIfAbsent(property.name) {
            createJavaFunctionGetter<T, R>(property)
        } as Function<T, R>
    }

    @Suppress("UNCHECKED_CAST")
    fun <T, R> createJavaFunctionGetter(
        property: KProperty1<T, R>
    ): Function<Any, Any?> {
        val javaGetter = property.javaGetter
            ?: throw IllegalArgumentException("Property ${property.name} has no getter")

        val lookup = ReflectionHandles.lookupFor(javaGetter.declaringClass)
        val handle = lookup.unreflect(javaGetter)

        return Function { instance ->
            handle.invokeWithArguments(instance)
        }
    }

    /**
     * Creates a Java BiConsumer setter
     */
    inline fun <reified T, V> createPropertySetter(
        property: KProperty1<T, V>
    ): BiConsumer<T, V> {
        val metadata = appReflection.getMetadata(T::class)

        @Suppress("UNCHECKED_CAST")
        return metadata.setterLambdas.computeIfAbsent(property.name) {
            createJavaBiConsumerSetter<T, V>(property)
        } as BiConsumer<T, V>
    }

    @Suppress("UNCHECKED_CAST")
    fun <T, V> createJavaBiConsumerSetter(
        property: KProperty1<T, V>
    ): BiConsumer<Any, Any?> {
        if (property !is KMutableProperty1<*, *>) {
            throw IllegalArgumentException("Property ${property.name} is not mutable")
        }

        val javaSetter = property.javaSetter
            ?: throw IllegalArgumentException("Property ${property.name} has no setter")

        val lookup = ReflectionHandles.lookupFor(javaSetter.declaringClass)
        val handle = lookup.unreflect(javaSetter)

        return BiConsumer { instance, value ->
            handle.invokeWithArguments(instance, value)
        }
    }

    // O(1) property access using pre-cached properties map
    fun getPropertyValue(instance: Any, propertyName: String): Any? {
        val metadata = appReflection.getMetadata(instance::class)

        val getter = metadata.getterLambdas.computeIfAbsent(propertyName) {
            val prop = metadata.properties[propertyName]
                ?: throw NoSuchFieldException("Property $propertyName not found")
            createJavaFunctionGetter(prop)
        }

        return getter.apply(instance)
    }

    fun setPropertyValue(instance: Any, propertyName: String, value: Any?) {
        val metadata = appReflection.getMetadata(instance::class)

        val setter = metadata.setterLambdas.computeIfAbsent(propertyName) {
            val prop = metadata.mutableProperties[propertyName]
                ?: throw NoSuchFieldException("Mutable property $propertyName not found")
            createJavaBiConsumerSetter(prop)
        }

        setter.accept(instance, value)
    }
}
