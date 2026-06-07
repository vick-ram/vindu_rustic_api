package org.example.reflection

import java.lang.reflect.Method
import kotlin.jvm.javaObjectType
import kotlin.jvm.javaPrimitiveType
import java.util.function.BiFunction
import java.util.function.Function

class LambdaMethodFactory {

    @PublishedApi
    internal val appReflection = AppReflection.INSTANCE

    /**
     * Creates a Java Function for method invocation with caching
     */
    inline fun <reified T : Any, reified R> createFunctionInvoker(
        methodName: String
    ): Function<T, R> {
        val metadata = appReflection.getMetadata(T::class)
        val method = ReflectionHandles.resolveMethod(T::class, methodName, emptyArray())
        val key = ReflectionHandles.signature(method)

        @Suppress("UNCHECKED_CAST")
        return metadata.methodLambdas.computeIfAbsent(key) {
            createMethodFunction(method)
        } as Function<T, R>
    }

    inline fun <reified T : Any, reified P1 : Any, reified R> createFunctionWithParam(
        methodName: String
    ): BiFunction<T, P1, R> {
        val metadata = appReflection.getMetadata(T::class)

        // Using an array or list depends on your resolveMethod signature requirements
        val parameterTypes = arrayOf(P1::class.javaPrimitiveType ?: P1::class.javaObjectType)
        val method = ReflectionHandles.resolveMethod(T::class, methodName, parameterTypes)
        val key = ReflectionHandles.signature(method)

        return BiFunction { instance, param ->
            val handle = metadata.methodLambdas.computeIfAbsent(key) {
                createMethodFunction(method)
            }

            @Suppress("UNCHECKED_CAST")
            handle.apply(arrayOf(instance, param)) as R
        }
    }

    @PublishedApi
    internal fun createMethodFunction(method: Method): Function<Array<Any?>, Any?> {
        val lookup = ReflectionHandles.lookupFor(method.declaringClass)
        val handle = lookup.unreflect(method)

        return Function { args ->
            try {
                handle.invokeWithArguments(args.toList())
            } catch (e: Throwable) {
                throw RuntimeException("Method invocation failed: ${method.name}", e)
            }
        }
    }
}
