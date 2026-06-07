package org.example.data.db.config

import io.r2dbc.spi.Connection
import io.r2dbc.spi.ConnectionFactory
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import org.reactivestreams.Publisher

class ReactiveDatabase(
    private val connectionFactory: ConnectionFactory
) {

    suspend fun <T> execute(block: suspend (Connection) -> T): T {
        // Use awaitSingle() because connection factories return exactly one Connection object
        val connection = connectionFactory.create().awaitSingle()
        return try {
            block(connection)
        } finally {
            // Use awaitFirstOrNull() because close() emits nothing (Publisher<Void>) on success
            connection.close().awaitFirstOrNull()
        }
    }

    suspend fun <T> transaction(block: suspend (Connection) -> T): T {
        val connection = connectionFactory.create().awaitSingle()
        return try {
            // Begin the transaction and wait for completion
            connection.beginTransaction().awaitFirstOrNull()

            val result = block(connection)

            // Commit transaction if the block finishes successfully
            connection.commitTransaction().awaitFirstOrNull()
            result
        } catch (e: Exception) {
            // Roll back if any error occurs within the transaction scope
            try {
                connection.rollbackTransaction().awaitFirstOrNull()
            } catch (rollbackError: Exception) {
                e.addSuppressed(rollbackError) // Preserve the original exception
            }
            throw e
        } finally {
            // Ensure connection resource cleanup
            connection.close().awaitFirstOrNull()
        }
    }
}
