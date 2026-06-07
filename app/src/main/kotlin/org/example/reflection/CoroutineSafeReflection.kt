package org.example.reflection

import kotlinx.coroutines.*
import java.lang.invoke.MethodHandle
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

class CoroutineSafeReflection {
    private val appReflection = AppReflection.INSTANCE

    // Suspending function invocations
    suspend fun invokeSuspending(
        instance: Any,
        methodName: String,
        vararg args: Any?
    ): Any? = withContext(Dispatchers.Default) {
        val method = ReflectionHandles.resolveMethod(instance::class, methodName, args)
        val handle = resolveMethodHandle(instance::class, method)
        handle.invokeWithArguments(listOf(instance) + args.toList())
    }

    private fun resolveMethodHandle(
        kClass: KClass<*>,
        method: java.lang.reflect.Method
    ): MethodHandle {
        val cacheEntry = appReflection.getMetadata(kClass)
        val key = ReflectionHandles.signature(method)

        return cacheEntry.methods.computeIfAbsent(key) {
            ReflectionHandles.lookupFor(method.declaringClass).unreflect(method)
        }
    }

    // Parallel property access using coroutines
    suspend fun <T : Any> getPropertiesParallel(
        instance: T,
        properties: List<KProperty1<T, *>>
    ): Map<String, Any?> = coroutineScope {
        properties.map { prop ->
            async {
                prop.name to PropertyCache.getProperty(instance, prop)
            }
        }.awaitAll().toMap()
    }
}
