package org.example.utils

import org.jetbrains.exposed.v1.core.ColumnType
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.QueryBuilder
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.append
import org.postgresql.util.PGobject

class TsVectorColumnType : ColumnType<String>() {
    override fun sqlType() = "TSVECTOR"

    override fun valueFromDB(value: Any): String {
        return when (value) {
            is PGobject -> value.value ?: ""
            else -> value.toString()
        }
    }

    override fun notNullValueToDB(value: String): Any {
            return PGobject().also {
                it.type = "tsvector"
                it.value = value
            }
    }
}

fun Table.tsVector(name: String): Column<String> = registerColumn(name, TsVectorColumnType())

fun Column<String>.customMatch(pattern: String): Op<Boolean> =
    object : Op<Boolean>() {
        override fun toQueryBuilder(queryBuilder: QueryBuilder) =
            queryBuilder {
                append(this@customMatch, " @@ to_tsquery('", pattern, ":*')")
            }
    }