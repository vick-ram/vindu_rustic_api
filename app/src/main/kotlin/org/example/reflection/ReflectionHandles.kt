package org.example.reflection

import java.lang.invoke.MethodHandles
import java.lang.reflect.Constructor
import java.lang.reflect.Method
import kotlin.reflect.KClass

data class ExecutableSignature(
    val name: String,
    val parameterTypes: List<Class<*>>
) {
    override fun toString(): String {
        return "$name(${parameterTypes.joinToString(",") { it.name }})"
    }
}

data class ConstructorSignature(
    val parameterTypes: List<Class<*>>
) {
    override fun toString(): String {
        return parameterTypes.joinToString(",") { it.name }
    }
}

object ReflectionHandles {
    private val primitiveToWrapper = mapOf<Class<*>, Class<*>>(
        java.lang.Boolean.TYPE to Boolean::class.java,
        java.lang.Byte.TYPE to Byte::class.java,
        Character.TYPE to Char::class.java,
        java.lang.Short.TYPE to Short::class.java,
        Integer.TYPE to Int::class.java,
        java.lang.Long.TYPE to Long::class.java,
        java.lang.Float.TYPE to Float::class.java,
        java.lang.Double.TYPE to Double::class.java,
        Void.TYPE to Void::class.java
    )

    fun lookupFor(declaringClass: Class<*>): MethodHandles.Lookup {
        return try {
            MethodHandles.privateLookupIn(declaringClass, MethodHandles.lookup())
        } catch (_: IllegalAccessException) {
            MethodHandles.lookup()
        }
    }

    fun signature(method: Method): ExecutableSignature {
        return ExecutableSignature(method.name, method.parameterTypes.toList())
    }

    fun signature(constructor: Constructor<*>): ConstructorSignature {
        return ConstructorSignature(constructor.parameterTypes.toList())
    }

    fun resolveMethod(
        kClass: KClass<*>,
        methodName: String,
        args: Array<out Any?>
    ): Method {
        val candidates = allMethods(kClass.java)
            .filter { it.name == methodName && it.parameterCount == args.size }
            .filter { method -> parametersAccept(method.parameterTypes, args) }

        return candidates.minWithOrNull(compareBy<Method> { conversionCost(it.parameterTypes, args) }
            .thenBy { it.declaringClass.name })
            ?: throw NoSuchMethodException(
                "No compatible method ${kClass.qualifiedName}.$methodName(${args.joinToString { it?.javaClass?.name ?: "null" }})"
            )
    }

    fun resolveMethod(
        kClass: KClass<*>,
        methodName: String,
        parameterTypes: List<Class<*>>
    ): Method {
        val candidates = allMethods(kClass.java)
            .filter { it.name == methodName && it.parameterCount == parameterTypes.size }
            .filter { method ->
                method.parameterTypes.zip(parameterTypes).all { (declared, requested) ->
                    declared.boxed().isAssignableFrom(requested.boxed()) ||
                        requested.boxed().isAssignableFrom(declared.boxed()) ||
                        numericWideningAccepts(declared.boxed(), requested.boxed())
                }
            }

        return candidates.minWithOrNull(compareBy<Method> { method ->
            method.parameterTypes.zip(parameterTypes).sumOf { (declared, requested) ->
                when {
                    declared.boxed() == requested.boxed() -> 0
                    declared.boxed().isAssignableFrom(requested.boxed()) -> 1
                    numericWideningAccepts(declared.boxed(), requested.boxed()) -> 2
                    else -> 4
                }
            }
        }.thenBy { it.declaringClass.name })
            ?: throw NoSuchMethodException(
                "No compatible method ${kClass.qualifiedName}.$methodName(${parameterTypes.joinToString { it.name }})"
            )
    }

    fun resolveConstructor(
        kClass: KClass<*>,
        args: Collection<Any?>
    ): Constructor<*> {
        val argArray = args.toTypedArray()
        val candidates = kClass.java.declaredConstructors
            .filter { it.parameterCount == argArray.size }
            .filter { constructor -> parametersAccept(constructor.parameterTypes, argArray) }

        return candidates.minWithOrNull(compareBy<Constructor<*>> { conversionCost(it.parameterTypes, argArray) })
            ?: throw NoSuchMethodException(
                "No compatible constructor ${kClass.qualifiedName}(${argArray.joinToString { it?.javaClass?.name ?: "null" }})"
            )
    }

    private fun parametersAccept(parameterTypes: Array<Class<*>>, args: Array<out Any?>): Boolean {
        return parameterTypes.zip(args).all { (parameterType, arg) -> accepts(parameterType, arg) }
    }

    private fun allMethods(type: Class<*>): List<Method> {
        val methods = mutableListOf<Method>()
        var current: Class<*>? = type
        while (current != null) {
            methods += current.declaredMethods
            current = current.superclass
        }
        methods += type.interfaces.flatMap { allMethods(it) }
        return methods.distinctBy { method ->
            Triple(method.name, method.returnType, method.parameterTypes.toList())
        }
    }

    private fun accepts(parameterType: Class<*>, arg: Any?): Boolean {
        if (arg == null) {
            return !parameterType.isPrimitive
        }

        val boxedParameter = parameterType.boxed()
        val argType = arg.javaClass

        return boxedParameter.isAssignableFrom(argType) || numericWideningAccepts(boxedParameter, argType)
    }

    private fun conversionCost(parameterTypes: Array<Class<*>>, args: Array<out Any?>): Int {
        return parameterTypes.zip(args).sumOf { (parameterType, arg) ->
            when {
                arg == null -> 4
                parameterType.boxed() == arg.javaClass -> 0
                parameterType.boxed().isAssignableFrom(arg.javaClass) -> 1
                numericWideningAccepts(parameterType.boxed(), arg.javaClass) -> 2
                else -> 10
            }
        }
    }

    private fun numericWideningAccepts(parameterType: Class<*>, argType: Class<*>): Boolean {
        val argRank = numericRank(argType) ?: return false
        val parameterRank = numericRank(parameterType) ?: return false
        return argRank <= parameterRank
    }

    private fun numericRank(type: Class<*>): Int? {
        return when (type.boxed()) {
            Byte::class.java -> 1
            Short::class.java -> 2
            Int::class.java -> 3
            Long::class.java -> 4
            Float::class.java -> 5
            Double::class.java -> 6
            else -> null
        }
    }

    private fun Class<*>.boxed(): Class<*> {
        return primitiveToWrapper[this] ?: this
    }
}
