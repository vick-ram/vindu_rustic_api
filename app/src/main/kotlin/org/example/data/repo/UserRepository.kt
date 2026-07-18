package org.example.data.repo

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

    override suspend fun create(model: User): User =
        super.create(model.copy(password = PasswordHasher.hash(model.password)))

    suspend fun searchUsers(query: String, offset: Int, limit: Int): List<User> =
        search(query = query, limit = limit, offset = offset)

    suspend fun findByEmail(email: String): User? =
        executeQuery("SELECT * FROM users WHERE email = :email", mapOf("email" to email), rowMapper)
            .firstOrNull()

    // -- was private, now public: raw row-mutation helpers belong here,
    // since only repo subclasses can reach connectionFactory/executeQuery

    suspend fun updateEmailVerification(userId: String, verified: Boolean) {
        val sql = "UPDATE users SET email_verified = :verified, updated_at = :updatedAt WHERE id = :userId"
        connectionFactory.useConnection {
            createStatement(sql)
                .bind("verified", verified)
                .bind("updatedAt", OffsetDateTime.now())
                .bind("userId", userId)
                .execute()
                .awaitSingle()
                .rowsUpdated
                .awaitSingle()
        }
    }

    suspend fun updatePhoneVerification(userId: String, verified: Boolean) {
        val sql = "UPDATE users SET phone_verified = :verified, updated_at = :updatedAt WHERE id = :userId"
        connectionFactory.useConnection {
            createStatement(sql)
                .bind("verified", verified)
                .bind("updatedAt", OffsetDateTime.now())
                .bind("userId", userId)
                .execute()
                .awaitSingle()
                .rowsUpdated
                .awaitSingle()
        }
    }

    suspend fun updatePassword(userId: String, newHashedPassword: String) {
        val sql = "UPDATE users SET password = :password, updated_at = :updatedAt WHERE id = :userId"
        connectionFactory.useConnection {
            createStatement(sql)
                .bind("password", newHashedPassword)
                .bind("updatedAt", OffsetDateTime.now())
                .bind("userId", userId)
                .execute()
                .awaitSingle()
                .rowsUpdated
                .awaitSingle()
        }
    }

    suspend fun update2FAStatus(userId: String, enabled: Boolean) {
        val sql = "UPDATE users SET enable_2fa = :enabled, updated_at = :updatedAt WHERE id = :userId"
        connectionFactory.useConnection {
            createStatement(sql)
                .bind("enabled", enabled)
                .bind("updatedAt", OffsetDateTime.now())
                .bind("userId", userId)
                .execute()
                .awaitSingle()
                .rowsUpdated
                .awaitSingle()
        }
    }

}