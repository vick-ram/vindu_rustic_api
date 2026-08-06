package org.example.reflection

import org.example.di.Inject
import org.example.di.Qualifier
import java.lang.reflect.Constructor
import java.util.concurrent.ConcurrentHashMap
import java.util.function.BiConsumer
import java.util.function.Supplier
import kotlin.reflect.KProperty1
import java.util.function.Function
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.isAccessible


data class ConstructorParam(val type: Class<*>, val qualifier: String?)

data class InjectableConstructor(
    val constructor: Constructor<*>,
    val parameters: List<ConstructorParam>
)

object Reflect {
    val propertyAccessor = PropertyAccessor()

    // ClassValue-based property cache - eliminates string key memory leaks
    val propertyCache = object : ClassValue<PropertyCacheEntry>() {
        override fun computeValue(type: Class<*>): PropertyCacheEntry {
            return PropertyCacheEntry()
        }
    }

    class PropertyCacheEntry {
        val getters = ConcurrentHashMap<String, Function<*, *>>()
        val setters = ConcurrentHashMap<String, BiConsumer<*, *>>()
    }

    // Cached, per-class: which constructor to use + each parameter's type/qualifier.
    // Computed once via kotlin-reflect, reused on every subsequent instantiation.
    private val constructorCache = object : ClassValue<Pair<Constructor<*>, Array<ConstructorParam>>>() {
        override fun computeValue(type: Class<*>): Pair<Constructor<*>, Array<ConstructorParam>> {
            // Find constructor annotated with @Inject, or fall back to primary/first constructor
            val constructor = type.constructors.find { c ->
                c.annotations.any { it.annotationClass.simpleName == "Inject" }
            } ?: type.constructors.firstOrNull()
            ?: throw IllegalStateException("No public constructor for ${type.name}")

            constructor.isAccessible = true

            val params = constructor.parameters.map { p ->
                val qualifier = p.annotations
                    .find { it.annotationClass.simpleName == "Qualifier" }
                    ?.let { ann ->
                        // Extract qualifier name dynamically
                        ann.javaClass.getMethod("name").invoke(ann) as? String
                    }
                ConstructorParam(p.type, qualifier)
            }.toTypedArray()

            return constructor to params
        }
    }
    fun getInjectableConstructor(clazz: KClass<*>): InjectableConstructor {
        val (javaConstructor, params) = constructorCache.get(clazz.java)
        return InjectableConstructor(javaConstructor, params.toList())
    }

    /**
     * Creates an instance of [clazz] using its cached injectable constructor.
     * [resolveParam] is called once per constructor parameter with (parameterType, qualifierNameOrNull)
     * and must return the value to pass in.
     */
    fun createInstance(clazz: KClass<*>, resolveParam: (KClass<*>, String?) -> Any): Any {
        val (javaConstructor, params) = constructorCache.get(clazz.java)
        val args = params.map { resolveParam(it.type.kotlin, it.qualifier) }.toTypedArray()

        // Using Java standard reflection avoids Kotlin KFunction's internal ClassLoader cast verification
        return javaConstructor.newInstance(*args)
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
