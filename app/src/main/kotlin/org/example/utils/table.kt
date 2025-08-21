package org.example.utils

import kotlinx.datetime.LocalDateTime
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.dao.Entity
import org.jetbrains.exposed.v1.dao.EntityChangeType
import org.jetbrains.exposed.v1.dao.EntityClass
import org.jetbrains.exposed.v1.dao.EntityHook
import org.jetbrains.exposed.v1.dao.toEntity
import org.jetbrains.exposed.v1.datetime.datetime

abstract class CustomTable(
    name: String = "",
    columnName: String = "id",
) : IdTable<String>(name) {
    private val randomUUID = varchar(columnName, length = 10)
        .clientDefault { shortUUID() }
        .uniqueIndex()

    override val id: Column<EntityID<String>> = randomUUID.entityId()
    val createdAt = datetime("created_at")
        .clientDefault { LocalDateTime.currentUtc() }
    val updatedAt = datetime("updated_at")
        .clientDefault { LocalDateTime.currentUtc() }
}

abstract class CustomEntity(
    id: EntityID<String>,
    table: CustomTable
): Entity<String>(id) {
    var createdAt by table.createdAt
    var updatedAt by table.updatedAt
}

abstract class CustomEntityClass<E: CustomEntity>(table: CustomTable) : EntityClass<String, E>(table) {
    init {
        EntityHook.subscribe { action ->
            if (action.changeType == EntityChangeType.Updated) {
                try {
                    action.toEntity(this)?.apply {
                        updatedAt = LocalDateTime.currentUtc()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    throw e
                }
            } else if (action.changeType == EntityChangeType.Created) {
                action.toEntity(this)?.apply {
                    createdAt = LocalDateTime.currentUtc()
                }
            }
        }
    }
}
