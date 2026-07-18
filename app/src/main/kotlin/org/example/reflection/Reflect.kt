package org.example.reflection

import org.example.di.Inject
import org.example.di.Qualifier
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


data class ConstructorParam(val type: KClass<*>, val qualifier: String?)

data class InjectableConstructor(
    val constructor: KFunction<Any>,
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
    private val constructorCache = object : ClassValue<InjectableConstructor>() {
        override fun computeValue(type: Class<*>): InjectableConstructor {
            val kClass = type.kotlin
            val constructors = kClass.constructors

            val selected = constructors.find { it.hasAnnotation<Inject>() }
                ?: kClass.primaryConstructor
                ?: constructors.firstOrNull()
                ?: throw IllegalStateException("No usable constructor for ${kClass.qualifiedName}")

            selected.isAccessible = true

            val params = selected.parameters.map { p ->
                val classifier = checkNotNull(p.type.classifier as KClass<*>) {
                    "Cannot resolve concrete parameter type for ${kClass.qualifiedName}, parameter '${p.name}'"
                }
                ConstructorParam(classifier, p.findAnnotation<Qualifier>()?.name)
            }

            return InjectableConstructor(selected, params)
        }
    }

    fun getInjectableConstructor(clazz: KClass<*>): InjectableConstructor =
        constructorCache.get(clazz.java)

    /**
     * Creates an instance of [clazz] using its cached injectable constructor.
     * [resolveParam] is called once per constructor parameter with (parameterType, qualifierNameOrNull)
     * and must return the value to pass in.
     */
    fun createInstance(clazz: KClass<*>, resolveParam: (KClass<*>, String?) -> Any): Any {
        val cached = getInjectableConstructor(clazz)
        val args = cached.parameters.map { resolveParam(it.type, it.qualifier) }
        return cached.constructor.call(*args.toTypedArray())
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
