package org.example.reflection

import java.lang.invoke.MethodHandle
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty1
import kotlin.reflect.jvm.javaGetter
import kotlin.reflect.jvm.javaSetter

object PropertyCache {
    private data class PropertyHandleKey(
        val declaringClass: Class<*>,
        val propertyName: String
    )

    private val PROPERTY_GETTER_CACHE = ConcurrentHashMap<PropertyHandleKey, MethodHandle>()
    private val PROPERTY_SETTER_CACHE = ConcurrentHashMap<PropertyHandleKey, MethodHandle>()

    @Suppress("UNCHECKED_CAST")
    fun <T : Any, R> getProperty(obj: T, property: KProperty1<T, R>): R {
        val javaGetter = property.javaGetter
            ?: throw IllegalArgumentException("Property ${property.name} has no getter")
        val key = PropertyHandleKey(javaGetter.declaringClass, property.name)
        val handle = PROPERTY_GETTER_CACHE.computeIfAbsent(key) {
            ReflectionHandles.lookupFor(javaGetter.declaringClass).unreflect(javaGetter)
        }
        return handle.invokeWithArguments(obj) as R
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Any, V> setProperty(obj: T, property: KProperty1<T, V>, value: V) {
        if (property !is KMutableProperty1<*, *>) {
            throw IllegalArgumentException("Property ${property.name} is not mutable")
        }
        val javaSetter = property.javaSetter
            ?: throw IllegalArgumentException("Property ${property.name} has no setter")
        val key = PropertyHandleKey(javaSetter.declaringClass, property.name)
        val handle = PROPERTY_SETTER_CACHE.computeIfAbsent(key) {
            ReflectionHandles.lookupFor(javaSetter.declaringClass).unreflect(javaSetter)
        }
        handle.invokeWithArguments(obj, value)
    }
}
