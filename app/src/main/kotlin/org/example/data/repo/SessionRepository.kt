package org.example.data.repo

import io.r2dbc.spi.ConnectionFactory
import kotlinx.coroutines.reactive.awaitSingle
import org.example.data.mappers.SessionMapper
import org.example.domain.models.identity.Session
import org.koin.core.annotation.Single

@Single
class SessionRepository(connectionFactory: ConnectionFactory, sessionMapper: SessionMapper) : CrudRepository<Session, String>(
    connectionFactory = connectionFactory,
    tableName = "sessions",
    mapper = sessionMapper
) {
    suspend fun findByRefreshToken(refreshToken: String): Session? {
        val sql = "SELECT * FROM sessions WHERE refresh_token = :refreshToken"
        return executeQuery(sql = sql, params = mapOf("refresh_token" to refreshToken), mapper = rowMapper)
            .firstOrNull()
    }

    suspend fun deleteAllForUser(userId: String) = connectionFactory.withTransaction { connection ->
        val sql = "DELETE FROM sessions WHERE user_id = :userId"
        connection.createStatement(sql)
            .bind("userId", userId)
            .execute()
            .awaitSingle()
            .rowsUpdated
            .awaitSingle()

        return@withTransaction
    }
}