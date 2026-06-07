package org.example.domain.repo

import org.example.domain.models.identity.TokenResponse
import org.example.domain.models.identity.User

interface UserRepository : CrudRepository<User, String> {
    suspend fun login(email: String, password: String): TokenResponse
    suspend fun logout(token: String): Boolean
    suspend fun searchUsers(query: String, offset: Int, limit: Int): List<User>
    suspend fun findByEmail(email: String): User?
}
