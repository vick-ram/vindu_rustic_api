package org.example.data.db.config

import com.google.gson.reflect.TypeToken
import kotlinx.datetime.LocalDateTime
import org.example.utils.GsonFactory
import org.example.utils.Json
import org.example.utils.currentUtc
import org.example.utils.shortUUID
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ColumnType
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.dao.Entity
import org.jetbrains.exposed.v1.dao.EntityChangeType
import org.jetbrains.exposed.v1.dao.EntityClass
import org.jetbrains.exposed.v1.dao.EntityHook
import org.jetbrains.exposed.v1.dao.toEntity
import org.jetbrains.exposed.v1.datetime.datetime
import org.postgresql.util.PGobject
import java.lang.reflect.Type

abstract class CustomTable(
    name: String = "",
    columnName: String = "id",
) : IdTable<String>(name) {

    private val randomUUID = varchar(columnName, length = 10)
        .clientDefault { shortUUID() }
        .uniqueIndex()

    override val id: Column<EntityID<String>> = randomUUID.entityId()
    val createdAt = datetime("created_at")
        .clientDefault { LocalDateTime.Companion.currentUtc() }
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

class GsonJsonColumnType<T: Any>(private val type: Type): ColumnType<T>() {
    private val gson = GsonFactory.gson

    override fun sqlType(): String = "JSONB"

    override fun valueFromDB(value: Any): T? {
        val json = when (value) {
            is PGobject -> value.value
            is String -> value
            else -> error("Unexpected value of type ${value::class.qualifiedName}")
        }
        return gson.fromJson<T>(json, type)
    }

    override fun notNullValueToDB(value: T): Any {
        val json = gson.toJson(value)
        return PGobject().apply {
            type = "jsonb"
            this.value = json
        }
    }

    override fun nonNullValueToString(value: T): String = gson.toJson(value)
}

inline fun <reified T: Any> Table.gsonJsonb(name: String): Column<T> {
    val type = object : TypeToken<T>() {}.type
    return registerColumn(name, GsonJsonColumnType(type))
}

