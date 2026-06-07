package org.example.utils

import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.RedisClient
import io.lettuce.core.RedisConnectionException
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.coroutines
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalLettuceCoroutinesApi::class)
object RedisService {

    private val client by lazy { RedisClient.create("redis://localhost:6379") }
    private var connection: StatefulRedisConnection<String, String>? = null
    val commands: RedisCoroutinesCommands<String, String>
        get() = getConnection().coroutines()

    private fun getConnection(): StatefulRedisConnection<String, String> {
        if (connection == null || !connection!!.isOpen) {
            connection = client.connect()
        }
        return connection!!
    }

    suspend fun healthCheck(): Boolean {
        return try {
            commands.ping() == "PONG"
        } catch (_: Exception) {
            false
        }
    }

    suspend fun <T> withRetry(
        maxRetries: Int = 3,
        block: suspend (RedisCoroutinesCommands<String, String>) -> T
    ): T {
        var retries = 0
        var lastException: Exception? = null

        while (retries < maxRetries) {
            try {
                val currentConnection = getConnection()
                return block(currentConnection.coroutines())
            } catch (e: Exception) {
                lastException = e
                retries++
                if (retries == maxRetries) break
                delay((1000 * retries.toLong()).milliseconds) // Exponential backoff

                // Reset connection on failure
                connection?.close()
                connection = null
            }
        }
        throw lastException ?: RedisConnectionException("Failed to connect after $maxRetries attempts")
    }
}