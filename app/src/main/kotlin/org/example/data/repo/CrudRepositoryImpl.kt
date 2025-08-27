package org.example.data.repo

import org.example.domain.repo.CrudRepository
import org.example.utils.CustomEntity
import org.example.utils.CustomEntityClass
import org.example.utils.suspendTransaction

abstract class CrudRepositoryImpl<T : CustomEntity, D>(private val entityClass: CustomEntityClass<T>) :
    CrudRepository<D, String> {
    abstract fun T.toDomain(): D
    abstract fun D.toEntity(entity: T)

    override suspend fun create(entity: D): D = suspendTransaction {
        entityClass.new {
            entity.toEntity(this)
        }.toDomain()
    }

    override suspend fun read(id: String): D? = suspendTransaction {
        entityClass.findById(id)?.toDomain()
    }

    override suspend fun readAll(
        offset: Int,
        limit: Int,
        queryParams: Map<String, String>?
    ): List<D> = suspendTransaction {
        entityClass.all()
            .limit(limit)
            .offset(offset.toLong())
            .filter { entity ->
                queryParams?.all { (key, value) ->
                    val property = entity::class.members.find { it.name == key }
                    property?.call(entity).toString().contains(value, ignoreCase = true)
                } == true
            }
            .map { it.toDomain() }
    }

    override suspend fun update(id: String, entity: D): D? = suspendTransaction {
        val ent = entityClass.findById(id) ?: return@suspendTransaction null
        entity.toEntity(ent)
        ent.toDomain()
    }

    override suspend fun delete(id: String): Boolean = suspendTransaction {
        val entity = entityClass.findById(id)
        entity?.delete()
        entity != null
    }
}