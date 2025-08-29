package org.example.data.repo

import org.example.data.db.entities.UserEntity
import org.example.data.db.tables.UserTable
import org.example.data.mappers.UserMapper
import org.example.domain.models.TokenResponse
import org.example.domain.models.User
import org.example.domain.repo.UserRepository
import org.example.plugins.AuthenticationException
import org.example.plugins.NotFoundException
import org.example.utils.HashPassword
import org.example.utils.blacklistToken
import org.example.utils.customMatch
import org.example.utils.makeJwtToken
import org.example.utils.suspendTransaction

class UserRepositoryImpl(private val userMapper: UserMapper) :
    CrudRepositoryImpl<UserEntity, User>(UserEntity, User::class),
    UserRepository {
    override suspend fun searchUsers(
        query: String,
        offset: Int,
        limit: Int
    ): List<User> = suspendTransaction {
        UserEntity.find { UserTable.tsv.customMatch(query) }
            .offset(offset.toLong())
            .limit(limit)
            .map { it.toDomain() }
    }

    override fun UserEntity.toDomain(): User = userMapper.toModel(this)

    override fun User.toEntity(entity: UserEntity) {
        userMapper.toEntity(this, entity)
    }

    override suspend fun login(
        email: String,
        password: String,
        issuer: String,
        audience: String,
        secret: String
    ): TokenResponse = suspendTransaction {
        val user = UserEntity.find { UserTable.email eq email }
            .firstOrNull() ?: throw NotFoundException("User with email $email not found")

        if (!HashPassword.verifyPassword(password, user.password)) {
            throw AuthenticationException("Invalid email or password")
        }

        val token = makeJwtToken(
            issuer = issuer,
            audience = audience,
            secret = secret,
            email = email,
            userId = user.id.value
        )

        TokenResponse(type = "Bearer", token = token!!)
    }

    override suspend fun logout(token: String): Boolean {
        blacklistToken(token = token)
        return true
    }
}
