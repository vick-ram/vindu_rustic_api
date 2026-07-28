package org.example.data.repo

import io.r2dbc.spi.Connection
import io.r2dbc.spi.ConnectionFactory
import kotlinx.coroutines.reactive.awaitSingle
import org.example.config.security.PasswordHasher
import org.example.data.mappers.UserMapper
import org.example.di.Component
import org.example.di.Inject
import org.example.domain.models.identity.User
import java.time.OffsetDateTime

@Component
class UserRepository @Inject constructor(
    connectionFactory: ConnectionFactory,
    userMapper: UserMapper
) :
    CrudRepository<User, String>(connectionFactory = connectionFactory, tableName = "users", mapper = userMapper) {

    override suspend fun create(model: User, connection: Connection?): User =
        super.create(model.copy(password = PasswordHasher.hash(model.password)), connection)

    suspend fun searchUsers(query: String, offset: Int, limit: Int): List<User> =
        search(query = query, limit = limit, offset = offset)

    suspend fun findByEmail(email: String): User? =
        executeQuery(sql = "SELECT * FROM users WHERE email = :email", params = mapOf("email" to email), mapper =  rowMapper)
            .firstOrNull()

    suspend fun updateEmailVerification(userId: String, verified: Boolean) {
        val sql = "UPDATE users SET email_verified = :verified, updated_at = :updatedAt WHERE id = :userId"
        connectionFactory.useConnection {
            createNamedStatement(sql, mapOf(
                "verified" to verified,
                "updatedAt" to OffsetDateTime.now(),
                "userId" to userId
            ))
                .execute()
                .awaitSingle()
                .rowsUpdated
                .awaitSingle()
        }
    }

    suspend fun updatePhoneVerification(userId: String, verified: Boolean) {
        val sql = "UPDATE users SET phone_verified = :verified, updated_at = :updatedAt WHERE id = :userId"
        connectionFactory.useConnection {
            createNamedStatement(sql, mapOf(
                "verified" to verified,
                "updatedAt" to OffsetDateTime.now(),
                "userId" to userId
            ))
                .execute()
                .awaitSingle()
                .rowsUpdated
                .awaitSingle()
        }
    }

    suspend fun updatePassword(userId: String, newHashedPassword: String) {
        val sql = "UPDATE users SET password = :password, updated_at = :updatedAt WHERE id = :userId"
        connectionFactory.useConnection {
            createNamedStatement(sql, mapOf(
                "password" to newHashedPassword,
                "updatedAt" to OffsetDateTime.now(),
                "userId" to userId
            ))
                .execute()
                .awaitSingle()
                .rowsUpdated
                .awaitSingle()
        }
    }

    suspend fun update2FAStatus(userId: String, enabled: Boolean) {
        val sql = "UPDATE users SET enable_2fa = :enabled, updated_at = :updatedAt WHERE id = :userId"
        connectionFactory.useConnection {
            createNamedStatement(sql, mapOf(
                "enabled" to enabled,
                "updatedAt" to OffsetDateTime.now(),
                "userId" to userId
            ))
                .execute()
                .awaitSingle()
                .rowsUpdated
                .awaitSingle()
        }
    }

}