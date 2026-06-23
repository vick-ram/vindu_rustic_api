package org.example.data.repo

import io.r2dbc.spi.Connection
import io.r2dbc.spi.ConnectionFactory
import io.r2dbc.spi.Row
import io.r2dbc.spi.RowMetadata
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import org.example.data.db.config.CustomEntity
import org.example.data.db.config.CustomEntityClass
import org.example.domain.repo.CrudRepository
import org.example.domain.repo.RowMapper
import org.example.plugins.AlreadyExistsException
import org.example.plugins.NotFoundException
import org.example.utils.suspendTransaction
import java.time.OffsetDateTime
import java.util.function.BiFunction
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
//        val ent = entityClass.findById(id) ?: throw NotFoundException("${domainClass.simpleName} not found")
//        entity.toEntity(ent)
//        ent.toDomain()

        entityClass.findByIdAndUpdate(id) {
            it.updatedAt = OffsetDateTime.now()
        }?.toDomain()
    }

    override suspend fun delete(id: String): Boolean = suspendTransaction {
        val entity = entityClass.findById(id) ?: throw NotFoundException("${domainClass.simpleName} not found")
        entity.delete()
        true
    }
}

data class JoinConfig(
    val tableName: String,
    val alias: String = tableName.take(1).lowercase(),
    val joinType: JoinType = JoinType.INNER,
    val onCondition: String,
    val columns: List<String> = listOf("*")
)

enum class JoinType {
    INNER, LEFT, RIGHT, FULL
}

class JoinedRowWrapper<T: Any>(
    private val mainMapper: RowMapper<Row, T>,
    private val joinMappers: Map<String, RowMapper<Row, *>> = emptyMap()
) {
    fun toModel(row: Row, metadata: RowMetadata): T {
        return mainMapper.toModel(row, metadata)
    }

    fun <J: Any> mapJoin(row: Row, metadata: RowMetadata, alias: String, mapper: RowMapper<Row, J>): J {
        return mapper.toModel(row, metadata)
    }
}

// Result wrapper for joined queries
data class JoinedResult<T: Any, J: Any>(
    val main: T,
    val joins: Map<String, J> = emptyMap()
)

abstract class R2dcCrudRepository<T: Any, ID: Any>(
    protected val connectionFactory: ConnectionFactory,
    protected val tableName: String,
    protected val idColumn: String = "id",
    protected val mapper: RowMapper<Row, T>,
    private val searchVectorColumn: String = "search_vector"
): CrudRepository<T, ID> {

    // Row mapping function
    protected open val rowMapper: BiFunction<Row, RowMetadata, T> = BiFunction {row, metadata -> mapper.toModel(row, metadata)}

    // Columns to exclude from INSERT/UPDATE that are auto-generated
    protected open val generatedColumns: List<String> = listOf("id", "created_at", "updated_at")

    // Columns returned after INSERT
    protected open val returningColumns: List<String> = listOf("*")

    override suspend fun create(entity: T): T {
        val rowData = mapper.toRow(entity)
        val columns = rowData.keys.filter { it !in generatedColumns }
        val placeholders = columns.joinToString(", ") { ":$it" }
        val columnNames = columns.joinToString(", ")

        val sql = """
            INSERT INTO $tableName ($columnNames)
            VALUES ($placeholders)
            RETURNING ${returningColumns.joinToString(", ")}
        """.trimIndent()

        return connectionFactory.withTransaction { connection ->
            val statement = connection.createStatement(sql)
            rowData.filterKeys { it in columns }.forEach { (key, value) ->
                if (value != null) {
                    statement.bind(key, value)
                }
            }
            statement.execute()
                .awaitSingle()
                .map(rowMapper)
                .awaitSingle()
        }
    }

    override suspend fun read(id: ID): T? {
        val sql = "SELECT * FROM $tableName WHERE $idColumn = :id"

        return connectionFactory.useConnection {
            createStatement(sql)
                .bind("id", id)
                .execute()
                .awaitSingle()
                .map(rowMapper)
                .awaitFirstOrNull()
        }
    }

    override suspend fun readAll(
        offset: Int,
        limit: Int,
        queryParams: Map<String, String>?
    ): List<T> {
        val whereClause = buildWhereClause(queryParams)
        val sql = """
            SELECT * FROM $tableName 
            $whereClause
            ORDER BY COALESCE(updated_at, created_at) DESC
            LIMIT :limit OFFSET :offset
        """.trimIndent()

        return connectionFactory.useConnection {
            val statement = createStatement(sql)
                .bind("limit", limit)
                .bind("offset", offset.toLong())

            queryParams?.forEach { (key, value) ->
                statement.bind(key, "%${value.lowercase()}%")
            }

            statement.execute()
                .awaitSingle()
                .map(rowMapper)
                .asFlow()
                .toList()
        }
    }

    override suspend fun update(id: ID, entity: T): T? {
        val rowData = mapper.toRow(entity)
        val setClauses = rowData.keys
            .filter { it != idColumn && it !in generatedColumns }
            .joinToString(", ") { "$it = :$it" }

        val sql = """
            UPDATE $tableName 
            SET $setClauses, updated_at = :updated_at 
            WHERE $idColumn = :id 
            RETURNING ${returningColumns.joinToString(", ")}
        """.trimIndent()

        return connectionFactory.withTransaction {connection ->
            val statement = connection.createStatement(sql)
                .bind("id", id)
                .bind("updated_at", OffsetDateTime.now())

            rowData.filterKeys { it != idColumn && it !in generatedColumns }
                .forEach { (key, value) ->
                    if (value != null) statement.bind(key, value) else statement.bindNull(key, Any::class.java)
                }

            statement.execute()
                .awaitSingle()
                .map(rowMapper)
                .awaitFirstOrNull()
        }
    }

    override suspend fun delete(id: ID): Boolean {
        val sql = "DELETE FROM $tableName WHERE $idColumn = :id"

        return connectionFactory.withTransaction { connection ->
            connection.createStatement(sql)
                .bind("id", id)
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
    ): List<T> {
        val tsQuery = buildTsQuery(query)
        val sql = """
            SELECT * FROM $tableName
            WHERE $searchVectorColumn @@ to_tsquery(:language, :query)
            ORDER BY ts_rank($searchVectorColumn, to_tsquery(:language, :query)) DESC
            LIMIT :limit OFFSET :offset
        """.trimIndent()

        return executeQuery(sql, mapOf(
            "language" to language,
            "query" to tsQuery,
            "limit" to limit,
            "offset" to offset
        ), rowMapper)
    }

    // Search with highlights
    suspend fun searchWithHighlights(
        query: String,
        highlightFields: List<String>
    ): List<Pair<T, Map<String, String>>> {
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
    ): List<T> {
        val sql = """
            SELECT * FROM $tableName
            WHERE $searchField ILIKE :prefix
            ORDER BY similarity($searchField, :exact_prefix) DESC
            LIMIT :limit
        """.trimIndent()

        return executeQuery(sql, mapOf(
            "prefix" to "$prefix%",
            "exact_prefix" to prefix,
            "limit" to limit
        ), rowMapper)
    }

    suspend fun advancedSearch(
        textQuery: String? = null,
        filters: Map<String, Any?> = emptyMap(),
        sortBy: String? = null,
        offset: Int = 0,
        limit: Int = 20
    ): List<T> {
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
            "LOWER(CAST($key AS TEXT)) LIKE LOWER(:$key)"
        }
    }

    // Helper method for custom queries
    protected suspend fun <R : Any> executeQuery(
        sql: String,
        params: Map<String, Any?> = emptyMap(),
        mapper: BiFunction<Row, RowMetadata, R> = BiFunction { row, _ -> row as R }
    ): List<R> {
        return connectionFactory.useConnection {
            val statement = createStatement(sql)
            params.forEach { (key, value) ->
                if (value != null) {
                    statement.bind(key, value)
                }
            }

            statement.execute()
                .awaitSingle() // Returns an R2DBC Result object
                .map(mapper)   // Returns a Publisher<R>
                .asFlow()      // Now works because R is guaranteed to be non-nullable
                .toList()
        }
    }

    // Single join
    suspend fun <J: Any> readWithJoin(
        id: ID,
        joinConfig: JoinConfig,
        joinMapper: RowMapper<Row, J>
    ): JoinedResult<T, J>? {
        val joinColumns = joinConfig.columns.joinToString(", ") {
            if (it == "*") "${joinConfig.alias}.*" else "${joinConfig.alias}.$it"
        }

        val sql = """
            SELECT $tableName.*, $joinColumns
            FROM $tableName
            ${joinConfig.joinType.name} JOIN ${joinConfig.tableName} ${joinConfig.alias}
            ON ${joinConfig.onCondition}
            WHERE $tableName.$idColumn = :id
        """.trimIndent()

        return connectionFactory.useConnection {
            createStatement(sql)
                .bind("id", id)
                .execute()
                .awaitSingle()
                .map { row, metadata ->
                    val main = mapper.toModel(row, metadata)
                    val join = joinMapper.toModel(row, metadata)
                    JoinedResult(main, mapOf(joinConfig.alias to join))
                }
                .awaitFirstOrNull()
        }
    }

    // Multiple joins
    suspend fun readWithJoins(
        id: ID,
        joinConfigs: List<JoinConfig>,
        joinMappers: Map<String, RowMapper<Row, *>>
    ): Map<String, Any>? {
        if (joinConfigs.isEmpty()) return null

        val selectParts = mutableListOf("$tableName.*")
        val joinClauses = mutableListOf<String>()

        joinConfigs.forEach { config ->
            val columns = config.columns.joinToString(", ") {
                if (it == "*") "${config.alias}.*" else "${config.alias}.$it"
            }
            selectParts.add(columns)
            joinClauses.add(
                "${config.joinType.name} JOIN ${config.tableName} ${config.alias} ON ${config.onCondition}"
            )
        }

        val sql = """
            SELECT ${selectParts.joinToString(", ")}
            FROM $tableName
            ${joinClauses.joinToString("\n")}
            WHERE $tableName.$idColumn = :id
        """.trimIndent()

        return connectionFactory.useConnection {
            createStatement(sql)
            .bind("id", id)
                .execute()
                .awaitSingle()
                .map { row, metadata ->
                    val result = mutableMapOf<String, Any>()
                    result["main"] = mapper.toModel(row, metadata)
                    joinMappers.forEach { (alias, m) ->
                        result[alias] = m.toModel(row, metadata) as Any
                    }
                    result
                }
                .awaitFirstOrNull()
        }
    }

    // Join with filtering and pagination
    suspend fun <J: Any> readAllWithJoin(
        joinConfig: JoinConfig,
        joinMapper: RowMapper<Row, J>,
        offset: Int = 0,
        limit: Int = 20,
        queryParams: Map<String, String>? = null,
        additionalConditions: Map<String, Any?> = emptyMap()
    ): List<Pair<T, Map<String, J?>>> {
        val joinColumns = joinConfig.columns.joinToString(", ") {
            if (it == "*") "${joinConfig.alias}.*" else "${joinConfig.alias}.$it"
        }

        val conditions = mutableListOf<String>()
        val params = mutableMapOf<String, Any?>()

        // Add query params conditions
        queryParams?.forEach { (key, value) ->
            conditions.add("LOWER(CAST($tableName.$key AS TEXT)) LIKE LOWER(:$key)")
            params[key] = "%${value.lowercase()}%"
        }

        // Add additional conditions (supports joined table columns)
        additionalConditions.forEach { (key, value) ->
            conditions.add("$key = :cond_${key.replace(".", "_")}")
            params["cond_${key.replace(".", "_")}"] = value
        }

        val whereClause = if (conditions.isEmpty()) ""
        else "WHERE ${conditions.joinToString(" AND ")}"

        val sql = """
            SELECT $tableName.*, $joinColumns
            FROM $tableName
            ${joinConfig.joinType.name} JOIN ${joinConfig.tableName} ${joinConfig.alias}
            ON ${joinConfig.onCondition}
            $whereClause
            ORDER BY $tableName.created_at DESC
            LIMIT :limit OFFSET :offset
        """.trimIndent()

        params["limit"] = limit
        params["offset"] = offset.toLong()

        return connectionFactory.useConnection {
            val statement = createStatement(sql)
            params.forEach { (key, value) ->
                if (value != null) statement.bind(key, value)
            }

            statement.execute()
                .awaitSingle()
                .map { row, metadata ->
                    val main = mapper.toModel(row, metadata)
                    val join = joinMapper.toModel(row,metadata)
                    main to mapOf(joinConfig.alias to join)
                }
                .asFlow()
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

        return connectionFactory.useConnection {
            val statement = createStatement(sql)
            params.forEach { (key, value) ->
                if (value != null) statement.bind(key, value)
            }
            limit?.let { statement.bind("limit", it) }
            offset?.let { statement.bind("offset", it.toLong()) }

            statement.execute()
                .awaitSingle()
                .map(mapper)
                .asFlow()
                .toList()
        }
    }

     suspend inline  fun <T> Connection.use(
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
}

