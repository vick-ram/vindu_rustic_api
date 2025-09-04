package org.example.domain.repo

import org.example.domain.models.TokenResponse
import org.example.domain.models.User

interface UserRepository : CrudRepository<User, String> {
    suspend fun login(email: String, password: String, issuer: String, audience: String, secret: String): TokenResponse
    suspend fun logout(token: String): Boolean
    suspend fun searchUsers(query: String, offset: Int, limit: Int): List<User>
    suspend fun findByEmail(email: String): User?
}