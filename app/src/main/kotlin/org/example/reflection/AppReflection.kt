package org.example.reflection

import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodType
import java.util.concurrent.ConcurrentHashMap
import java.util.function.BiConsumer
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties

class AppReflection {

    // Primary ClassValue-based cache - no string keys, no memory leaks
    private val classCache = object : ClassValue<ClassMetadata>() {
        override fun computeValue(type: Class<*>): ClassMetadata {
            return ClassMetadata.create(type.kotlin)
        }
    }

    data class ClassMetadata(
        val kClass: KClass<*>,
        // Pre-scanned properties for O(1) lookup
        val properties: Map<String, KProperty1<*, *>>,
        val mutableProperties: Map<String, KMutableProperty1<*, *>>,
        // Cached constructor factory
        val noArgConstructor: MethodHandle?,
        val allConstructors: Map<ConstructorSignature, MethodHandle>,
        // Lambda caches - store the actual Function/BiConsumer objects
        val getterLambdas: ConcurrentHashMap<String, java.util.function.Function<Any, Any?>> = ConcurrentHashMap(),
        val setterLambdas: ConcurrentHashMap<String, BiConsumer<Any, Any?>> = ConcurrentHashMap(),
        val methodLambdas: ConcurrentHashMap<ExecutableSignature, java.util.function.Function<Array<Any?>, Any?>> = ConcurrentHashMap(),
        val methods: ConcurrentHashMap<ExecutableSignature, MethodHandle> = ConcurrentHashMap()
    ) {
        companion object {
            fun create(kClass: KClass<*>): ClassMetadata {
                val lookup = ReflectionHandles.lookupFor(kClass.java)

                // Pre-scan all properties once
                val allProps = kClass.memberProperties.associateBy { it.name }
                val mutableProps = allProps.filterValues { it is KMutableProperty1<*, *> }
                    .mapValues { it.value as KMutableProperty1<*, *> }

                // Cache constructors
                val noArgCtor = try {
                    lookup.findConstructor(kClass.java, MethodType.methodType(Void.TYPE))
                } catch (e: NoSuchMethodException) {
                    null
                }

                val ctors = kClass.java.declaredConstructors.associate { ctor ->
                    val key = ReflectionHandles.signature(ctor)
                    val handle = lookup.unreflectConstructor(ctor)
                    key to handle
                }

                return ClassMetadata(
                    kClass = kClass,
                    properties = allProps,
                    mutableProperties = mutableProps,
                    noArgConstructor = noArgCtor,
                    allConstructors = ctors
                )
            }
        }
    }

    fun getMetadata(kClass: KClass<*>): ClassMetadata {
        return classCache.get(kClass.java)
    }

    companion object {
        val INSTANCE = AppReflection()
    }
}
