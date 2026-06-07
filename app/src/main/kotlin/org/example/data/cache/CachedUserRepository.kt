package org.example.data.cache

import org.example.domain.models.identity.TokenResponse
import org.example.domain.models.identity.User
import org.example.domain.repo.CrudRepository
import org.example.domain.repo.UserRepository

class CachedUserRepository(
    private val delegate: UserRepository,
    private val cache: CrudRepository<User, String>
): UserRepository {
    override suspend fun login(
        email: String,
        password: String
    ): TokenResponse {
        return delegate.login(email, password)
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