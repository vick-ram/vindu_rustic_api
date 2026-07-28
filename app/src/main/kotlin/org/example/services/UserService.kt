package org.example.services

import org.example.data.cache.UserCache
import org.example.data.repo.CrudCache
import org.example.di.Inject
import org.example.di.Injectable
import org.example.domain.models.identity.TokenResponse
import org.example.domain.models.identity.User
import java.net.InetAddress

@Injectable
class UserService @Inject constructor(private val userCache: UserCache, private val authService: AuthService) {

    suspend fun createUser(user: User): User {
        return userCache.create(user)
    }

    suspend fun login(
        email: String,
        password: String,
        ipAddress: InetAddress? = null,
        deviceInfo: String? = null,
    ): TokenResponse {
        return authService.login(email, password, ipAddress, deviceInfo)
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

    suspend fun logout(userId: String, accessToken: String): Boolean {
        authService.logout(userId, accessToken)
        return true
    }
}