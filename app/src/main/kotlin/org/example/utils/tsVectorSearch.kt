package org.example.utils

import org.jetbrains.exposed.v1.core.ColumnType
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Expression
import org.jetbrains.exposed.v1.core.ExpressionWithColumnType
import org.jetbrains.exposed.v1.core.FloatColumnType
import org.jetbrains.exposed.v1.core.IColumnType
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.QueryBuilder
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.append
import org.jetbrains.exposed.v1.core.stringLiteral
import org.postgresql.util.PGobject


private const val TS_VECTOR_SQL_TYPE = "tsvector"

fun Expression<*>.customMatch(query: String): Op<Boolean> = TsVectorMatchOp(this, query)

class TsVectorMatchOp(
    private val column: Expression<*>,
    private val query: String
): Op<Boolean>() {
    override fun toQueryBuilder(queryBuilder: QueryBuilder) = queryBuilder {
        append(column, " @@ plainto_tsquery('english', ", stringLiteral(sanitizeQuery(query)), ")")
    }
}

class TsVectorColumnType : ColumnType<String>(nullable = true) {
    override fun sqlType(): String = TS_VECTOR_SQL_TYPE

    override fun valueFromDB(value: Any): String {
        return value as String
    }

    override fun valueToDB(value: String?): Any? {
        return PGobject().apply {
            type = TS_VECTOR_SQL_TYPE
            this.value = value
        }.value
    }

    override fun notNullValueToDB(value: String): Any {
        return PGobject().apply {
            type = TS_VECTOR_SQL_TYPE
            this.value = value
        }
    }

    override fun nonNullValueToString(value: String): String {
        return "'$value'"
    }
}

fun Table.tsVector(name: String): Column<String> = registerColumn(name, TsVectorColumnType())

//fun Column<String>.customMatch(pattern: String): Op<Boolean> =
//    object : Op<Boolean>() {
//        override fun toQueryBuilder(queryBuilder: QueryBuilder) =
//            queryBuilder {
//                append(this@customMatch, " @@ to_tsquery('", pattern, ":*')")
//                append(stringLiteral(sanitizeQuery(pattern)))
//            }
//    }

fun sanitizeQuery(query: String): String {
    return query.trim().replace(Regex("\\s+"), " & ")
        .replace(Regex("[!|&:*()']"), " ")
        .replace(Regex("\\s+"), " & ")
        .trim('&', ' ') + ":*"
}