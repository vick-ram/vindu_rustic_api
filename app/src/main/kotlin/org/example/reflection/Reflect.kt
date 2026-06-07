package org.example.reflection

import java.util.concurrent.ConcurrentHashMap
import java.util.function.BiConsumer
import java.util.function.Supplier
import kotlin.reflect.KProperty1
import java.util.function.Function

object Reflect {
    val propertyAccessor = PropertyAccessor()

    // ClassValue-based property cache - eliminates string key memory leaks
    val propertyCache = object : ClassValue<PropertyCacheEntry>() {
        override fun computeValue(type: Class<*>): PropertyCacheEntry {
            return PropertyCacheEntry()
        }
    }

    class PropertyCacheEntry {
        val getters = ConcurrentHashMap<String, java.util.function.Function<*, *>>()
        val setters = ConcurrentHashMap<String, BiConsumer<*, *>>()
    }

    /**
     * Cached property getter backed by a MethodHandle adapter.
     */
    inline fun <reified T : Any, reified R> getProperty(
        obj: T,
        property: KProperty1<T, R>
    ): R {
        val cache = propertyCache.get(obj.javaClass)

        @Suppress("UNCHECKED_CAST")
        val getter = cache.getters.computeIfAbsent(property.name) {
            // Cast to wildcard representation for storage
            propertyAccessor.createPropertyGetter(property) as java.util.function.Function<*, *>
        } as Function<T, R>

        return getter.apply(obj)
    }

    /**
     * Cached property setter backed by a MethodHandle adapter.
     */
    inline fun <reified T : Any, V> setProperty(
        obj: T,
        property: KProperty1<T, V>,
        value: V
    ) {
        val cache = propertyCache.get(obj.javaClass)

        @Suppress("UNCHECKED_CAST")
        val setter = cache.setters.computeIfAbsent(property.name) {
            // Cast to wildcard representation for storage
            propertyAccessor.createPropertySetter(property) as BiConsumer<*, *>
        } as BiConsumer<T, V>

        setter.accept(obj, value)
    }

    /**
     * Creates a fast constructor supplier
     */
    inline fun <reified T : Any> createConstructor(): Supplier<T> {
        val metadata = AppReflection.INSTANCE.getMetadata(T::class)
        val ctor = metadata.noArgConstructor
            ?: throw IllegalArgumentException("No no-arg constructor for ${T::class.simpleName}")

        return Supplier {
            @Suppress("UNCHECKED_CAST")
            ctor.invokeWithArguments(emptyList<Any?>()) as T
        }
    }

    inline fun <reified T : Any> builder(): FastBuilder<T> {
        return FastBuilder(T::class)
    }
}
