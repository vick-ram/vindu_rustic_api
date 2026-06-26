package org.example.domain.repo

interface CrudRepository_<Model, ID> {
    suspend fun create(model: Model): Model
    suspend fun read(id: ID): Model?
    suspend fun readAll(
        offset: Int,
        limit: Int,
        queryParams: Map<String, String>?,
    ): List<Model>
    suspend fun update(id: ID, model: Model): Model?
    suspend fun delete(id: ID): Boolean
}