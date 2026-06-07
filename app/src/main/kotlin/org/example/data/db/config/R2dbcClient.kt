// config/R2dbcClient.kt
package org.example.data.db.config

import io.r2dbc.spi.Connection
import io.r2dbc.spi.Row
import io.r2dbc.spi.Statement
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.singleOrNull
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitSingle
import java.util.UUID

class R2dbcClient(private val connection: Connection) {

    suspend fun execute(cmd: SqlCommand): Long {
        return createStatement(cmd)
            .execute()                  // Returns Publisher<Result>
            .awaitSingle()              // Suspends until the single Result is available
            .rowsUpdated                // Returns Publisher<Long>
            .awaitSingle()              // Suspends until the row count is available
    }

    suspend fun <T : Any> queryOne(cmd: SqlCommand, mapper: (Row) -> T): T? {
        // We leverage queryFlow underneath and safely consume the first element or null
        return queryFlow(cmd, mapper).singleOrNull()
    }

    suspend fun <T : Any> query(cmd: SqlCommand, mapper: (Row) -> T): List<T> {
        // We leverage queryFlow underneath and gather all elements non-blockingly into a List
        return queryFlow(cmd, mapper).toList()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun <T : Any> queryFlow(cmd: SqlCommand, mapper: (Row) -> T): Flow<T> {
        return createStatement(cmd)
            .execute()                  // Returns Publisher<Result>
            .asFlow()                   // Converts outer Publisher to Flow<Result>
            .flatMapConcat { result ->
                result.map { row, _ -> mapper(row) } // Returns Publisher<T>
                    .asFlow()                        // Converts inner Publisher to Flow<T>
            }
    }

    private fun createStatement(cmd: SqlCommand): Statement {
        val statement = connection.createStatement(cmd.sql)
        cmd.params.forEach { (key, value) ->
            bindValue(statement, key, value)
        }
        return statement
    }

    private fun bindValue(statement: Statement, key: String, value: Any?) {
        if (value == null) {
            // Note: Some R2DBC drivers prefer the explicit column type. 
            // String::class.java acts as a safe fallback for common relational DBs.
            statement.bindNull(key, String::class.java)
            return
        }

        when (value) {
            is String -> statement.bind(key, value)
            is Int -> statement.bind(key, value)
            is Long -> statement.bind(key, value)
            is UUID -> statement.bind(key, value)
            is Boolean -> statement.bind(key, value)
            is java.time.Instant -> statement.bind(key, value)
            is java.time.LocalDateTime -> statement.bind(key, value)
            is java.math.BigDecimal -> statement.bind(key, value)
            is Enum<*> -> statement.bind(key, value.name)
            else -> statement.bind(key, value.toString())
        }
    }
}
