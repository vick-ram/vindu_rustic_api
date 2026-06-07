package org.example.services

import org.example.domain.models.identity.TokenResponse
import org.example.domain.models.identity.User
import org.example.domain.repo.UserRepository
import org.example.plugins.AuthenticationException
import org.example.utils.HashPassword

class UserService(private val userRepository: UserRepository, tokenService: TokenService) {

    suspend fun createUser(user: User): User {
        return userRepository.create(user)
    }

    suspend fun login(
        email: String,
        password: String
    ): TokenResponse {
        return userRepository.login(email, password)
    }

    suspend fun updateUser(id: String, user: User): User? {
        return userRepository.update(id, user)
    }

    suspend fun getUsers(offset: Int = 0, limit: Int = 10, queryParams: Map<String, String>?): List<User> {
        return userRepository.readAll(offset, limit, queryParams)
    }

    suspend fun getUser(id: String): User? {
        return userRepository.read(id)
    }

    suspend fun getUserByEmail(email: String): User? {
        return userRepository.findByEmail(email)
    }

    suspend fun authenticate(email: String, password: String): User? {
        val user = this.getUserByEmail(email)
            ?: throw AuthenticationException("User not found")

        if (!HashPassword.verifyPassword(password, user.password)) {
            throw AuthenticationException("Invalid password")
        }

        if (!user.active) {
            throw AuthenticationException("Account deactivated")
        }

        return user
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