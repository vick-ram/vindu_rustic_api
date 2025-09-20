package org.example.domain.repo

import org.example.domain.models.TokenResponse
import org.example.domain.models.User

interface UserRepository : CrudRepository<User, String> {
    suspend fun login(email: String, password: String, issuer: String, audience: String, secret: String): TokenResponse
    suspend fun logout(token: String): Boolean
    suspend fun searchUsers(query: String, offset: Int, limit: Int): List<User>
    suspend fun findByEmail(email: String): User?
}

class CachedUserRepository(
    private val delegate: UserRepository,
    private val cache: CrudRepository<User, String>
): UserRepository {
    override suspend fun login(
        email: String,
        password: String,
        issuer: String,
        audience: String,
        secret: String
    ): TokenResponse {
        return delegate.login(email, password, issuer, audience, secret)
    }

    override suspend fun logout(token: String): Boolean {
        return delegate.logout(token)
    }

    override suspend fun searchUsers(
        query: String,
        offset: Int,
        limit: Int
    ): List<User> {
        return delegate.searchUsers(query, offset, limit)
    }

    override suspend fun findByEmail(email: String): User? {
        return delegate.findByEmail(email)
    }

    override suspend fun create(entity: User): User {
        return cache.create(entity)
    }

    override suspend fun read(id: String): User? {
        return cache.read(id)
    }

    override suspend fun readAll(
        offset: Int,
        limit: Int,
        queryParams: Map<String, String>?
    ): List<User> {
        return cache.readAll(offset, limit, queryParams)
    }

    override suspend fun update(
        id: String,
        entity: User
    ): User? {
        return cache.update(id, entity)
    }

    override suspend fun delete(id: String): Boolean {
        return cache.delete(id)
    }
}
