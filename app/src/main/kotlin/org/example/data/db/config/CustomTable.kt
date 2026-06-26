package org.example.data.db.config

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.datetime.timestampWithTimeZone
import java.time.OffsetDateTime
import java.util.concurrent.ConcurrentLinkedQueue

abstract class CustomTable(
    name: String = "",
    columnName: String = "id",
    indexTimestamps: Boolean = true
) : IdTable<String>(name) {

    override val id: Column<EntityID<String>> = varchar(columnName, length = 26)
        .clientDefault { Ulid.generate() }
        .default(Ulid.generate())
        .uniqueIndex()
        .entityId()

    val createdAt = timestampWithTimeZone("created_at")
        .apply { if (indexTimestamps) index() }
        .clientDefault { OffsetDateTime.now() }
        .default(OffsetDateTime.now())

    val updatedAt = timestampWithTimeZone("updated_at")
        .apply { if (indexTimestamps) index() }
        .clientDefault { OffsetDateTime.now() }
        .default(OffsetDateTime.now())

    companion object {
        private val _tables = ConcurrentLinkedQueue<Table>()
        val tables: Array<Table> get() = _tables.toTypedArray()
    }

    init {
        _tables.add(this)
    }
}

