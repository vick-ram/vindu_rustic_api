package org.example.data.repo

import org.example.data.db.config.CustomEntity
import org.example.data.db.config.CustomEntityClass
import org.example.domain.repo.CrudRepository
import org.example.plugins.AlreadyExistsException
import org.example.plugins.NotFoundException
import org.example.utils.suspendTransaction
import kotlin.reflect.KClass

abstract class CrudRepositoryImpl<T : CustomEntity, D : Any>(
    private val entityClass: CustomEntityClass<T>,
    private val domainClass: KClass<D>
) :
    CrudRepository<D, String> {
    abstract fun T.toDomain(): D
    abstract fun D.toEntity(entity: T)
    abstract fun getId(domain: D): String

    override suspend fun create(entity: D): D = suspendTransaction {
        val id = getId(entity)
        val exists = entityClass.findById(id)
        if (exists != null) {
            throw AlreadyExistsException("${domainClass::simpleName} already exists")
        }
        entityClass.new {
            entity.toEntity(this)
        }.toDomain()
    }

    override suspend fun read(id: String): D? = suspendTransaction {
        val result = entityClass.findById(id) ?: throw NotFoundException("${domainClass.simpleName} not found")
        return@suspendTransaction result.toDomain()
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
            .sortedBy { it.createdAt.coerceAtLeast(it.updatedAt) }
            .map { it.toDomain() }
    }

    override suspend fun update(id: String, entity: D): D? = suspendTransaction {
        val ent = entityClass.findById(id) ?: throw NotFoundException("${domainClass.simpleName} not found")
        entity.toEntity(ent)
        ent.toDomain()
    }

    override suspend fun delete(id: String): Boolean = suspendTransaction {
        val entity = entityClass.findById(id) ?: throw NotFoundException("${domainClass.simpleName} not found")
        entity.delete()
        true
    }
}