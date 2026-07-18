package org.example.di

import io.github.classgraph.ClassGraph
import io.ktor.server.application.*
import io.ktor.server.engine.applicationEnvironment
import org.example.reflection.Reflect
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.core.scope.Scope
import org.koin.dsl.binds
import org.koin.dsl.module
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation

class AutoWireScanner(
    private val application: Application,
    private val basePackages: List<String>
) {

    /**
     * Scans [basePackages] for @Injectable / @Component classes.
     * Returns a single Koin module registering all of them.
     */
    fun scanAndCreateModules(): List<Module> {
        val injectableClasses = scanPackages()

        val autoWireModule = module {
            injectableClasses.forEach { clazz ->
                try {
                    registerBean(clazz)
                } catch (e: Exception) {
                    // A single misconfigured bean (e.g. unresolvable constructor param)
                    // should not prevent the rest of the app from wiring up.
                    application.log.error("AutoWireScanner: failed to register ${clazz.qualifiedName}: ${e.message}")
                }
            }
        }

        return listOf(autoWireModule)
    }

    private fun scanPackages(): Set<KClass<*>> {
        ClassGraph()
            .enableAnnotationInfo()
            .acceptPackages(*basePackages.toTypedArray())
            .scan().use { result ->
                val annotated = result.getClassesWithAnnotation(Injectable::class.java.name)
                    .union(result.getClassesWithAnnotation(Component::class.java.name))

                return annotated
                    .filter { !it.isInterface && !it.isAbstract }
                    .mapNotNull { classInfo ->
                        try {
                            classInfo.loadClass().kotlin
                        } catch (e: Throwable) {
                            // Skip classes that fail to load (missing optional deps, etc.)
                            application.log.error("AutoWireScanner: could not load ${classInfo.name}: ${e.message}")
                            null
                        }
                    }
                    .toSet()
            }
    }

    private fun Module.registerBean(clazz: KClass<*>) {
        val qualifier = clazz.findAnnotation<Qualifier>()?.name

        // Bind to implemented interfaces; fall back to the concrete class itself
        // if it implements none, so it's still resolvable by its own type.
        val boundTypes: List<KClass<*>> = clazz.java.interfaces
            .map { it.kotlin }
            .ifEmpty { listOf(clazz) }

        val definition = if (qualifier != null) {
            single(named(qualifier)) { buildInstance(clazz) }
        } else {
            single { buildInstance(clazz) }
        }

        definition.binds(boundTypes.toTypedArray())
    }

    private fun Scope.buildInstance(clazz: KClass<*>): Any =
        Reflect.createInstance(clazz) { paramType, paramQualifier ->
            resolveDependency(paramType, paramQualifier)
        }

    private fun Scope.resolveDependency(type: KClass<*>, qualifier: String?): Any =
        if (qualifier != null) {
            getKoin().get(type, named(qualifier))
        } else {
            getKoin().get(type)
        }
}
