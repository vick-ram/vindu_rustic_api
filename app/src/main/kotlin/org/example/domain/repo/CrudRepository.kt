package org.example.domain.repo

interface CrudRepository<T, ID> {
    suspend fun create(entity: T): T
    suspend fun read(id: ID): T?
    suspend fun readAll(
        offset: Int,
        limit: Int,
        queryParams: Map<String, String>?,
    ): List<T>
    suspend fun update(id: ID, entity: T): T?
    suspend fun delete(id: ID): Boolean
}