package org.example.di

import io.ktor.server.application.*
import org.apache.hc.core5.util.Identifiable
import org.example.domain.repo.CrudRepository
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger
import kotlin.reflect.KClass

fun Application.configureDI() {
    install(Koin) {
        slf4jLogger()
        modules(roleModule, userModule, cartModule, orderModule, productModule, frontendModule)
    }
}

inline fun <reified Entity : Any, reified ID, reified RepoImpl, reified RepoInterface>
        Module.registerRepository(
    qualifier: String,
    repositoryImpl: KClass<RepoImpl>,
    cacheClass: KClass<out CrudRepository<Entity, ID>>? = null
) where RepoImpl : CrudRepository<Entity, ID>,
        RepoInterface : CrudRepository<Entity, ID> {
    val realQualifier = "${qualifier}Real"
    val cacheQualifier = "${qualifier}Cache"

    // Real repository
    single<CrudRepository<Entity, ID>>(named(realQualifier)) {
        repositoryImpl.constructors.first().call(get())
    }

    single<RepoInterface>(named(realQualifier)) {
        get<CrudRepository<Entity, ID>>(named(realQualifier)) as RepoInterface
    }

    // Cached repository (if cache is provided)
    cacheClass?.let { cacheClazz ->
        single<CrudRepository<Entity, ID>>(named(cacheQualifier)) {

            val constructor = cacheClazz.constructors.first()

            val args = when {
                constructor.parameters.size >= 6 -> arrayOf(
                    get(named(realQualifier)),        // delegate: CrudRepository<T, ID>
                    Entity::class.java,              // clazz: Class<T>
                    { entity: Entity ->               // getId: (T) -> ID
                        // You'll need to handle ID extraction differently
                        // Let's create a more flexible approach
                        extractId<Entity, ID>(entity, qualifier)
                    },
                    "$qualifier-cache",              // cacheName: String?
                    get(),                          // logger: Logger (injected)
                    null                            // ttl: Long? = null
                )

                constructor.parameters.size >= 4 -> arrayOf(
                    get(named(realQualifier)),
                    Entity::class.java,
                    { entity: Entity -> extractId<Entity, ID>(entity, qualifier) },
                    "$qualifier-cache"
                )

                else -> arrayOf(
                    get(named(realQualifier)),
                    Entity::class.java,
                    { entity: Entity -> extractId<Entity, ID>(entity, qualifier) }
                )
            }
            constructor.call(*args)
        }
    }
}

// Helper function to extract ID safely
inline fun <reified Entity : Any, reified ID> extractId(entity: Entity, qualifier: String): ID {
    return when (entity) {
        is Identifiable -> entity.id as ID
        else -> {
            try {
                val idField = entity::class.java.declaredFields.find { it.name == "id" }
                    ?: throw IllegalArgumentException("Entity $qualifier must have an 'id' field or implement Identifiable")
                idField.isAccessible = true
                idField.get(entity) as ID
            } catch (e: Exception) {
                throw IllegalArgumentException("Could not extract ID from entity $qualifier: ${e.message}")
            }
        }
    }
}
