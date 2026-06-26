package org.example.config.security

import at.favre.lib.crypto.bcrypt.BCrypt
import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Single
import java.security.SecureRandom

enum class OtpPurpose {
    EMAIL_VERIFICATION,
    PHONE_VERIFICATION,
    PASSWORD_RESET,
    LOGIN_2FA,
    ACCOUNT_DELETION
}

@OptIn(ExperimentalLettuceCoroutinesApi::class)
@Single
class OtpService(
    private val redis: RedisCoroutinesCommands<String, String>
) {
    private val otpLength = 6
    private val otpTtlMinutes = 5L
    private val maxAttempts = 3

    data class OtpData(
        val otp: String,
        val attempts: Int = 0,
        val purpose: OtpPurpose,
        val createdAt: Long = System.currentTimeMillis()
    )

    suspend fun generateOtp(userId: String, purpose: OtpPurpose): String {
            val otp = generateRandomOtp()
            val otpData = OtpData(
                otp = hashOtp(otp), // Store hashed OTP
                purpose = purpose,
                createdAt = System.currentTimeMillis()
            )

            val key = "otp:${purpose.name}:$userId"
            redis.apply {
                setex(key, otpTtlMinutes * 60, Json.encodeToString(otpData))
                // Set attempt counter
                setex("$key:attempts", otpTtlMinutes * 60, "0")
            }

            return otp // Return plain OTP only for sending
    }

    suspend fun verifyOtp(userId: String, purpose: OtpPurpose, otp: String): OtpVerificationResult {
            val key = "otp:${purpose.name}:$userId"
        val storedData = redis.get(key) ?: return OtpVerificationResult.Expired

        val attempts = (redis.get("$key:attempts")?.toIntOrNull() ?: 0) + 1

            if (attempts > maxAttempts) {
                // Invalidate OTP after max attempts
                redis.del(key, "$key:attempts")
                return OtpVerificationResult.TooManyAttempts
            }

        redis.setex("$key:attempts", otpTtlMinutes * 60, attempts.toString())

            val otpData = Json.decodeFromString<OtpData>(storedData)

            if (!verifyOtpHash(otp, otpData.otp)) {
                return OtpVerificationResult.Invalid(
                    remainingAttempts = maxAttempts - attempts
                )
            }

            // OTP verified, clean up
        redis.del(key, "$key:attempts")

            return OtpVerificationResult.Success
    }

    suspend fun canResendOtp(userId: String, purpose: OtpPurpose): Boolean {
            val key = "otp:resend:${purpose.name}:$userId"
            val lastSent = redis.get(key)

            if (lastSent != null) {
                val timeSinceLastSent = System.currentTimeMillis() - lastSent.toLong()
                return timeSinceLastSent > 60_000 // 1 minute cooldown
            }

        redis.setex(key, 60, System.currentTimeMillis().toString())
            return true
    }

    private fun generateRandomOtp(): String {
        val random = SecureRandom()
        return (0 until otpLength).map { random.nextInt(10) }.joinToString("")
    }

    private fun hashOtp(otp: String): String {
        return BCrypt.withDefaults().hashToString(12, otp.toCharArray())
    }

    private fun verifyOtpHash(otp: String, hash: String): Boolean {
        return BCrypt.verifyer().verify(otp.toCharArray(), hash).verified
    }
}

sealed class OtpVerificationResult {
    object Success : OtpVerificationResult()
    object Expired : OtpVerificationResult()
    data class Invalid(val remainingAttempts: Int) : OtpVerificationResult()
    object TooManyAttempts : OtpVerificationResult()
}