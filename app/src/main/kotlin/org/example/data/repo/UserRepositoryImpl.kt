package org.example.data.repo

import org.example.data.db.entities.UserEntity
import org.example.data.db.tables.Users
import org.example.data.mappers.UserMapper
import org.example.domain.models.identity.TokenResponse
import org.example.domain.models.identity.User
import org.example.domain.repo.UserRepository
import org.example.plugins.AuthenticationException
import org.example.plugins.NotFoundException
import org.example.services.TokenService
import org.example.utils.HashPassword
import org.example.utils.customMatch
import org.example.utils.suspendTransaction

class UserRepositoryImpl(private val userMapper: UserMapper, private val tokenService: TokenService) :
    CrudRepositoryImpl<UserEntity, User>(UserEntity, User::class),
    UserRepository {
    override suspend fun searchUsers(
        query: String,
        offset: Int,
        limit: Int
    ): List<User> = suspendTransaction {
        UserEntity.find { Users.tsv.customMatch(query) }
            .offset(offset.toLong())
            .limit(limit)
            .map { it.toDomain() }
    }

    override suspend fun findByEmail(email: String): User? = suspendTransaction {
        UserEntity.find { Users.email.eq(email) }
            .firstOrNull()
            ?.toDomain()
    }

    override fun UserEntity.toDomain(): User = userMapper.toModel(this)

    override fun User.toEntity(entity: UserEntity) {
        userMapper.toEntity(this, entity)
    }

    override fun getId(domain: User): String = domain.id

    override suspend fun login(
        email: String,
        password: String
    ): TokenResponse = suspendTransaction {
        val user = UserEntity.find { Users.email eq email }
            .firstOrNull() ?: throw NotFoundException("User with email $email not found")

        if (!HashPassword.verifyPassword(password, user.password)) {
            throw AuthenticationException("Invalid email or password")
        }
        val claims = mapOf(
            "userId" to user.id,
            "email" to user.email
        )

        tokenService.makeJwtToken(claims)
    }

    override suspend fun logout(token: String): Boolean {
        tokenService.blacklistToken(token)
        return true
    }
}
