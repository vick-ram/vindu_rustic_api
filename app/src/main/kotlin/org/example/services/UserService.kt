package org.example.services

import org.example.domain.models.TokenResponse
import org.example.domain.models.User
import org.example.domain.repo.UserRepository

class UserService(private val userRepository: UserRepository) {

    suspend fun createUser(user: User): User {
        return userRepository.create(user)
    }

    suspend fun login(
        email: String,
        password: String,
        issuer: String,
        audience: String,
        secret: String
    ): TokenResponse {
        return userRepository.login(email, password, issuer, audience, secret)
    }

    suspend fun updateUser(id: String, user: User): User? {
        return userRepository.update(id, user)
    }

    suspend fun getUsers(offset: Int = 0, limit: Int = 10, queryParams: Map<String, String>): List<User> {
        return userRepository.readAll(offset, limit, queryParams)
    }

    suspend fun getUser(id: String): User? {
        return userRepository.read(id)
    }

    suspend fun searchUsers(query: String, offset: Int, limit: Int): List<User> {
        return userRepository.searchUsers(query, offset, limit)
    }

    suspend fun deleteUser(id: String): Boolean {
        return userRepository.delete(id)
    }

    suspend fun logout(token: String): Boolean {
        return userRepository.logout(token)
    }
}