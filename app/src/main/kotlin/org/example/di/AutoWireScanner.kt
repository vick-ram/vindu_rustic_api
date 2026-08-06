package org.example.di

import io.github.classgraph.ClassGraph
import io.ktor.server.application.*
import org.example.reflection.Reflect
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.core.scope.Scope
import org.koin.dsl.binds
import org.koin.dsl.module
import kotlin.reflect.KClass

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
                    application.log.error("AutoWireScanner: failed to register ${clazz.qualifiedName}: ${e.message}")
                }
            }
        }

        return listOf(autoWireModule)
    }

    private fun scanPackages(): Set<KClass<*>> {
        val classLoader = application::class.java.classLoader
        return try {
            val scanResult = ClassGraph()
                .enableAllInfo()
                .overrideClassLoaders(classLoader)
                .acceptPackages(*basePackages.toTypedArray())
                .scan()

            scanResult.use { result ->
                val injectableAnnotated = result.getClassesWithAnnotation(Injectable::class.java.name)
                val componentAnnotated = result.getClassesWithAnnotation(Component::class.java.name)
                val annotated = injectableAnnotated.union(componentAnnotated)

                annotated
                    .filter { !it.isInterface && !it.isAbstract }
                    .mapNotNull { classInfo ->
                        try {
                           classLoader.loadClass(classInfo.name).kotlin
                        } catch (e: Throwable) {
                            System.err.println("Failed to load ${classInfo.name}: ${e.message}")
                            null
                        }
                    }
                    .toSet()
            }
        } catch (e: Exception) {
            System.err.println("ClassGraph scan FAILED:")
            e.printStackTrace()
            emptySet()
        }
    }

    private fun Module.registerBean(clazz: KClass<*>) {
        val qualifier = clazz.java.annotations
            .find { it.annotationClass.simpleName == "Qualifier" }
            ?.let { it.javaClass.getMethod("name").invoke(it) as? String }

        try {
            val interfaces = clazz.java.interfaces.map { it.kotlin }
            val boundTypes = (listOf(clazz) + interfaces).toTypedArray()

            val definition = if (qualifier != null) {
                single(named(qualifier)) { buildInstance(clazz) }
            } else {
                single { buildInstance(clazz) }
            }
            definition.binds(boundTypes)
        } catch (e: Exception) {
            application.log.error("✗ Registration FAILED for ${clazz.qualifiedName}: ${e.message}")
        }
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
