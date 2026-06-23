package org.example.domain.repo

import org.example.config.security.OtpPurpose
import org.example.config.security.OtpService
import org.example.data.repo.Enable2FAResult
import org.example.data.repo.ResendResult
import org.example.data.repo.ResetPasswordResult
import org.example.data.repo.VerificationResult
import org.example.domain.models.identity.TokenResponse
import org.example.domain.models.identity.User
import java.net.InetAddress

interface UserRepository : CrudRepository<User, String> {
    suspend fun login(email: String, password: String, ipAddress: InetAddress? = null, deviceInfo: String? = null): TokenResponse
    suspend fun logout(userId: String, accessToken: String): Boolean
    suspend fun searchUsers(query: String, offset: Int, limit: Int): List<User>
    suspend fun findByEmail(email: String): User?
    suspend fun updateEmailVerification(userId: String, verified: Boolean)
    suspend fun updatePhoneVerification(userId: String, verified: Boolean)
    suspend fun updatePassword(userId: String, oldPassword: String, newPassword: String)
    suspend fun verifyEmail(userId: String, otp: String): VerificationResult
    suspend fun verifyPhone(userId: String, otp: String): VerificationResult
    suspend fun resendOtp(userId: String, purpose: OtpPurpose): ResendResult
    suspend fun requestPasswordReset(email: String)
    suspend fun resetPassword(email: String, otp: String, newPassword: String): ResetPasswordResult
    suspend fun verify2FA(userId: String, otp: String): VerificationResult
    suspend fun update2FAStatus(userId: String, enabled: Boolean)
}
