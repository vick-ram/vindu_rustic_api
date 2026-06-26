package org.example.services

import org.example.data.cache.UserCache
import org.example.data.repo.CrudCache
import org.example.domain.models.identity.TokenResponse
import org.example.domain.models.identity.User
import java.net.InetAddress

class UserService(private val userCache: UserCache) {

    suspend fun createUser(user: User): User {
        return userCache.create(user)
    }

    suspend fun login(
        email: String,
        password: String,
        ipAddress: InetAddress? = null,
        deviceInfo: String? = null,
    ): TokenResponse {
        return userCache.login(email, password, ipAddress, deviceInfo)
    }

    suspend fun updateUser(id: String, user: User): User? {
        return userCache.update(id, user)
    }

    suspend fun getUsers(offset: Int = 0, limit: Int = 10, queryParams: Map<String, String>?): List<User> {
        return userCache.readAll(offset, limit, queryParams)
    }

    suspend fun getUser(id: String): User? {
        return userCache.read(id)
    }

    suspend fun getUserByEmail(email: String): User? {
        return userCache.readByEmail(email)
    }

    suspend fun searchUsers(query: String, offset: Int, limit: Int): List<User> {
        return userCache.searchUsers(query, offset, limit)
    }

    suspend fun deleteUser(id: String): Boolean {
        return userCache.delete(id)
    }

    suspend fun logout(userId: String ,accessToken: String): Boolean {
        userCache.logout(userId, accessToken)
        return true
    }
}