package org.example.data.repo

import io.r2dbc.spi.ConnectionFactory
import kotlinx.coroutines.reactive.awaitSingle
import org.example.data.mappers.SessionMapper
import org.example.di.Component
import org.example.di.Inject
import org.example.domain.models.identity.Session

@Component
class SessionRepository @Inject constructor(connectionFactory: ConnectionFactory, sessionMapper: SessionMapper) :
    CrudRepository<Session, String>(
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
        connection.createNamedStatement(sql, mapOf("userId" to userId))
            .execute()
            .awaitSingle()
            .rowsUpdated
            .awaitSingle()

        return@withTransaction
    }
}