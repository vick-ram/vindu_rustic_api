package org.example.utils

import org.example.data.repo.CrudCache
import org.example.data.repo.CrudRepositoryImpl
import org.example.domain.repo.CrudRepository
import java.io.File

inline fun <reified T: Any, reified E: CustomEntity> createCrudCache(
    entityClass: CustomEntityClass<E>,
    storageDir: File = File("app/build/cache"),
    noinline getId: (T) -> String,
    crossinline toDomain: E.() -> T,
    crossinline toEntity: T.(E) -> Unit
): CrudRepository<T, String> {
    return CrudCache(
        delegate = object : CrudRepositoryImpl<E, T>(entityClass) {
            override fun E.toDomain(): T = toDomain()

            override fun T.toEntity(entity: E) = toEntity(entity)
        },
        clazz = T::class.java,
        idClazz = String::class.java,
        storageFile = storageDir,
        getId = getId
    )
}