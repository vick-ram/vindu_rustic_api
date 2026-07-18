package org.example.data.repo

import io.r2dbc.spi.Connection
import io.r2dbc.spi.ConnectionFactory
import io.r2dbc.spi.Row
import io.r2dbc.spi.RowMetadata
import io.r2dbc.spi.Statement
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import org.example.domain.repo.RowMapper
import java.time.OffsetDateTime
import java.util.function.BiFunction


object NamedParamSql {
    private val PARAM_REGEX = Regex(":(\\w+)(?!:)")

    data class Converted(val sql: String, val order: List<String>)

    fun convert(sql: String): Converted {
        val order = mutableListOf<String>()
        val rewritten = PARAM_REGEX.replace(sql) { m ->
            val name = m.groupValues[1]
            order += name
            "$$${order.size}" // Explicit sequential indexing: $1, $2, $3...
        }
        return Converted(rewritten, order)
    }
}

/** Wrap nullable values so bindNull gets the right type — r2dbc has no way to infer it from null alone. */
data class TypedNull(val type: Class<*>)

fun Statement.bindNamedParams(order: List<String>, params: Map<String, Any?>): Statement {
    order.forEachIndexed { i, name ->
        // Handle null tracking safely via containsKey or fallback
        if (!params.containsKey(name)) error("Missing binding for :$name")
        when (val value = params[name]) {
            is TypedNull -> this.bindNull(i, value.type)
            null -> this.bindNull(i, String::class.java)
            else -> this.bind(i, value)
        }
    }
    return this
}

fun Connection.createNamedStatement(sql: String, params: Map<String, Any?>): Statement {
    val (positionalSql, order) = NamedParamSql.convert(sql)
    val statement = this.createStatement(positionalSql)
    return statement.bindNamedParams(order, params)
}

abstract class CrudRepository<Model : Any, ID : Any>(
    protected val connectionFactory: ConnectionFactory,
    protected val tableName: String,
    protected val idColumn: String = "id",
    protected val mapper: RowMapper<Row, Model>,
    private val searchVectorColumn: String = "search_vector"
) {

    // Row mapping function
    protected open val rowMapper: BiFunction<Row, RowMetadata, Model> =
        BiFunction { row, metadata -> mapper.toModel(row, metadata) }

    // Columns to exclude from INSERT/UPDATE that are auto-generated
    protected open val generatedColumns: List<String> = listOf("id", "created_at", "updated_at")

    // Columns returned after INSERT
    protected open val returningColumns: List<String> = listOf("*")

    open suspend fun create(model: Model): Model {
        val rowData = mapper.toRow(model)
        val columns = rowData.keys.filter { it !in generatedColumns }
        val placeholders = columns.joinToString(", ") { ":$it" }
        val columnNames = columns.joinToString(", ")

        val sql = """
            INSERT INTO $tableName ($columnNames)
            VALUES ($placeholders)
            RETURNING ${returningColumns.joinToString(", ")}
        """.trimIndent()

        return connectionFactory.withTransaction { connection ->
            val params = rowData.filterKeys { it in columns }
            connection.createNamedStatement(sql, params)
                .execute()
                .awaitSingle()
                .map(rowMapper)
                .awaitSingle()
        }
    }

    open suspend fun read(id: ID): Model? {
        val sql = "SELECT * FROM $tableName WHERE $idColumn = :id"
        val params = mapOf<String, Any?>("id" to id)

        return connectionFactory.useConnection {
            createNamedStatement(sql, params)
                .execute()
                .awaitSingle()
                .map(rowMapper)
                .awaitFirstOrNull()
        }
    }

    open suspend fun readAll(
        offset: Int,
        limit: Int,
        queryParams: Map<String, String>?
    ): List<Model> {
        val whereClause = buildWhereClause(queryParams)
        val sql = """
            SELECT * FROM $tableName 
            $whereClause
            ORDER BY COALESCE(updated_at, created_at) DESC
            LIMIT :limit OFFSET :offset
        """.trimIndent()

        val params = mutableMapOf<String, Any?>(
            "limit" to limit,
            "offset" to offset.toLong()
        )

        queryParams?.forEach { (key, value) ->
            params["filter_$key"] = "%${value.lowercase()}%" // Fixed: Avoid key namespace collisions
        }

        return connectionFactory.useConnection {
            createNamedStatement(sql, params)
                .execute()
                .awaitSingle()
                .map(rowMapper)
                .asFlow()
                .toList()
        }
    }

    open suspend fun update(id: ID, model: Model): Model? {
        val rowData = mapper.toRow(model)
        val setClauses = rowData.keys
            .filter { it != idColumn && it !in generatedColumns }
            .joinToString(", ") { "$it = :$it" }

        val sql = """
            UPDATE $tableName 
            SET $setClauses, updated_at = :updated_at 
            WHERE $idColumn = :id 
            RETURNING ${returningColumns.joinToString(", ")}
        """.trimIndent()

        return connectionFactory.withTransaction { connection ->
            val params = mutableMapOf<String, Any?>(
                "id" to id,
                "updated_at" to OffsetDateTime.now()
            )
            params.putAll(rowData.filterKeys { it != idColumn && it !in generatedColumns })

            connection.createNamedStatement(sql, params)
                .execute()
                .awaitSingle()
                .map(rowMapper)
                .awaitFirstOrNull()
        }
    }

    open suspend fun delete(id: ID): Boolean {
        val sql = "DELETE FROM $tableName WHERE $idColumn = :id"
        val params = mapOf<String, Any?>("id" to id)

        return connectionFactory.withTransaction { connection ->
            connection.createNamedStatement(sql, params)
                .execute()
                .awaitSingle()
                .rowsUpdated
                .awaitSingle() > 0
        }
    }

    // Full-text search with ranking
    suspend fun search(
        query: String,
        language: String = "english",
        offset: Int = 0,
        limit: Int = 20
    ): List<Model> {
        val tsQuery = buildTsQuery(query)
        val sql = """
            SELECT * FROM $tableName
            WHERE $searchVectorColumn @@ to_tsquery(:language, :query)
            ORDER BY ts_rank($searchVectorColumn, to_tsquery(:language, :query)) DESC
            LIMIT :limit OFFSET :offset
        """.trimIndent()

        return executeQuery(
            sql, mapOf(
                "language" to language,
                "query" to tsQuery,
                "limit" to limit,
                "offset" to offset
            ), rowMapper
        )
    }

    // Search with highlights
    suspend fun searchWithHighlights(
        query: String,
        highlightFields: List<String>
    ): List<Pair<Model, Map<String, String>>> {
        val tsQuery = buildTsQuery(query)
        val highlightParts = highlightFields.joinToString(", ") { field ->
            "ts_headline('english', $field, to_tsquery(:query)) as ${field}_highlight"
        }

        val sql = """
            SELECT *, $highlightParts
            FROM $tableName
            WHERE $searchVectorColumn @@ to_tsquery(:query)
            ORDER BY ts_rank($searchVectorColumn, to_tsquery(:query)) DESC
            LIMIT 20
        """.trimIndent()

        return executeQuery(sql, mapOf("query" to tsQuery)) { row, metadata ->
            val entity = rowMapper.apply(row, metadata)
            val highlights = highlightFields.associateWith { field ->
                row.get("${field}_highlight", String::class.java) ?: ""
            }
            entity to highlights
        }
    }

    // Autocomplete/suggestions using trigram similarity
    suspend fun suggest(
        prefix: String,
        searchField: String = "name",
        limit: Int = 10
    ): List<Model> {
        val sql = """
            SELECT * FROM $tableName
            WHERE $searchField ILIKE :prefix
            ORDER BY similarity($searchField, :exact_prefix) DESC
            LIMIT :limit
        """.trimIndent()

        return executeQuery(
            sql, mapOf(
                "prefix" to "$prefix%",
                "exact_prefix" to prefix,
                "limit" to limit
            ), rowMapper
        )
    }

    suspend fun advancedSearch(
        textQuery: String? = null,
        filters: Map<String, Any?> = emptyMap(),
        sortBy: String? = null,
        offset: Int = 0,
        limit: Int = 20
    ): List<Model> {
        val conditions = mutableListOf<String>()
        val params = mutableMapOf<String, Any?>()

        textQuery?.let { query ->
            conditions.add("$searchVectorColumn @@ to_tsquery(:ts_query)")
            params["ts_query"] = buildTsQuery(query)
        }

        // Dynamic filters (safe because you control the keys)
        filters.forEach { (key, value) ->
            conditions.add("$key = :filter_$key")
            params["filter_$key"] = value
        }

        val whereClause = if (conditions.isEmpty()) ""
        else "WHERE ${conditions.joinToString(" AND ")}"

        val orderClause = when (sortBy) {
            "relevance" -> if (textQuery != null)
                "ORDER BY ts_rank($searchVectorColumn, to_tsquery(:ts_query)) DESC"
            else "ORDER BY created_at DESC"

            "newest" -> "ORDER BY created_at DESC"
            "oldest" -> "ORDER BY created_at ASC"
            else -> "ORDER BY COALESCE(updated_at, created_at) DESC"
        }

        val sql = """
            SELECT * FROM $tableName
            $whereClause
            $orderClause
            LIMIT :limit OFFSET :offset
        """.trimIndent()

        params["limit"] = limit
        params["offset"] = offset

        return executeQuery(sql, params, rowMapper)
    }

    private fun buildTsQuery(query: String): String {
        return query.split("\\s+".toRegex())
            .filter { it.isNotBlank() }
            .joinToString(" & ") { "$it:*" }
    }

    protected open fun buildWhereClause(queryParams: Map<String, String>?): String {
        if (queryParams.isNullOrEmpty()) return ""

        return "WHERE " + queryParams.entries.joinToString(" AND ") { (key, _) ->
            "LOWER(CAST($key AS TEXT)) LIKE LOWER(:filter_$key)"
        }
    }

    // Helper method for custom queries
    protected suspend fun <R : Any> executeQuery(
        sql: String,
        params: Map<String, Any?> = emptyMap(),
        mapper: BiFunction<Row, RowMetadata, R> = BiFunction { row, _ -> row as R }
    ): List<R> {
        return connectionFactory.useConnection {
            val (positionalSql, order) = NamedParamSql.convert(sql)
            val statement = createStatement(positionalSql)
            statement.bindNamedParams(order, params)

            statement.execute()
                .awaitSingle() // Returns an R2DBC Result object
                .map(mapper)   // Returns a Publisher<R>
                .asFlow()      // Now works because R is guaranteed to be non-nullable
                .toList()
        }
    }

    // Generic join method for complex queries
    protected suspend fun <R : Any> executeJoinQuery(
        selectClause: String,
        joinClauses: List<String>,
        whereClause: String = "",
        orderClause: String = "",
        params: Map<String, Any?> = emptyMap(),
        limit: Int? = null,
        offset: Int? = null,
        mapper: BiFunction<Row, RowMetadata, R>
    ): List<R> {
        val sql = buildString {
            append("SELECT $selectClause")
            append("\nFROM $tableName")
            joinClauses.forEach { append("\n$it") }
            if (whereClause.isNotBlank()) append("\nWHERE $whereClause")
            if (orderClause.isNotBlank()) append("\n$orderClause")
            limit?.let { append("\nLIMIT :limit") }
            offset?.let { append("\nOFFSET :offset") }
        }

        val allParams = params.toMutableMap()
        limit?.let { allParams["limit"] = it }
        offset?.let { allParams["offset"] = it.toLong() }

        return connectionFactory.useConnection {
            val (positionalSql, order) = NamedParamSql.convert(sql)
            val statement = createStatement(positionalSql)
            statement.bindNamedParams(order, allParams)

            statement.execute()
                .awaitSingle()
                .map(mapper)
                .asFlow()
                .toList()
        }
    }

    suspend inline fun <T> Connection.use(
        block: suspend Connection.() -> T
    ): T {
        return try {
            block()
        } finally {
            close().awaitFirstOrNull()
        }
    }

    suspend inline fun <T> ConnectionFactory.useConnection(
        crossinline block: suspend Connection.() -> T
    ): T {
        return create()
            .awaitSingle()
            .use {
                block()
            }
    }

    suspend inline fun <T> ConnectionFactory.withTransaction(
        block: suspend (Connection) -> T
    ): T {
        val conn = create().awaitSingle()
        return try {
            // Begin transaction
            conn.beginTransaction().awaitFirstOrNull()
            val result = block(conn)

            // commit
            conn.commitTransaction().awaitFirstOrNull()
            result
        } catch (e: Exception) {
            try {
                // roll back
                conn.rollbackTransaction().awaitFirstOrNull()
            } catch (rollbackError: Exception) {
                e.addSuppressed(rollbackError)
            }
            throw e
        } finally {
            conn.close().awaitFirstOrNull()
        }
    }

    fun <T : Any> Statement.bindNullable(name: String, value: T?, type: Class<T>): Statement =
        value?.let { bind(name, it) } ?: bind(name, type)
}

