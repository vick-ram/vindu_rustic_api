package org.example.data.cache

import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands
import kotlinx.serialization.KSerializer
import org.example.config.security.OtpPurpose
import org.example.data.mappers.UserMapper
import org.example.data.repo.CacheConfig
import org.example.data.repo.CrudCache
import org.example.data.repo.ResendResult
import org.example.data.repo.ResetPasswordResult
import org.example.data.repo.UserRepository
import org.example.data.repo.VerificationResult
import org.example.domain.models.identity.TokenResponse
import org.example.domain.models.identity.User
import org.koin.core.annotation.Single
import java.net.InetAddress

@OptIn(ExperimentalLettuceCoroutinesApi::class)
@Single
class UserCache(
    redis: RedisCoroutinesCommands<String, String>,
    val userRepository: UserRepository,
    userMapper: UserMapper,
    serializer: KSerializer<User>,
    config: CacheConfig
): CrudCache<User, String>(
    redis = redis,
    delegate = userRepository,
    getId = { user -> userMapper.getId(user) as String },
    serializer = serializer,
    config = config
) {
    suspend fun login(email: String, password: String, ipAddress: InetAddress?, deviceInfo: String?): TokenResponse {
        return userRepository.login(
            email, password, ipAddress, deviceInfo
        )
    }

    suspend fun refreshTokens(refreshToken: String) : TokenResponse {
        return userRepository.refreshTokens(refreshToken)
    }

    suspend fun readByEmail(email: String): User? {
        return userRepository.findByEmail(email)
    }

    suspend fun searchUsers(query: String, offset: Int, limit: Int): List<User> {
        return userRepository.searchUsers(query, offset, limit)
    }

    suspend fun updatePassword(userId: String, oldPassword: String, newPassword: String) {
        userRepository.updatePassword(userId, oldPassword, newPassword)
    }

    suspend fun update2FAStatus(userId: String, enabled: Boolean) {
        userRepository.update2FAStatus(userId, enabled)
    }

    suspend fun verifyPhone(userId: String, otp: String): VerificationResult {
        return userRepository.verifyPhone(userId, otp)
    }

    suspend fun verifyEmail(userId: String, otp: String): VerificationResult {
        return userRepository.verifyEmail(userId, otp)
    }

    suspend fun resendOtp(userId: String, purpose: OtpPurpose): ResendResult {
        return userRepository.resendOtp(userId, purpose)
    }

    suspend fun resetPassword(email: String, otp: String, newPassword: String): ResetPasswordResult {
        return userRepository.resetPassword(email, otp, newPassword)
    }

    suspend fun logout(userId: String, accessToken: String) {
        userRepository.logout(userId, accessToken)
    }
}