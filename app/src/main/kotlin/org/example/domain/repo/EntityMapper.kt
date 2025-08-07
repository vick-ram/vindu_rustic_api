package org.example.domain.repo

interface EntityMapper<Entity: org.jetbrains.exposed.v1.dao.Entity<ID>, Model, ID : Comparable<ID>> {
    fun toModel(entity: Entity): Model
    fun toEntity(model: Model, entity: Entity): Entity
}