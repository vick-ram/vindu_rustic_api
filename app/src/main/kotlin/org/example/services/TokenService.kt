package org.example.services

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands
import org.example.config.AppConfig
import org.example.domain.models.identity.TokenResponse
import java.util.*

@OptIn(ExperimentalLettuceCoroutinesApi::class)
class TokenService(
    private val redis: RedisCoroutinesCommands<String, String>,
    private val config: AppConfig
) {

    private val ACCESS_TOKEN_EXPIRY = 3_600_000L // 1 hour
    private val REFRESH_TOKEN_EXPIRY = 604_800_000L // 7 days

    fun makeJwtToken(additionalClaims: Map<String, Any>): TokenResponse {
        val accessToken = generateToken(tokenType = "access", additionalClaims)
        val refreshToken = generateToken(tokenType = "refresh", additionalClaims)

        return TokenResponse(
            type = "bearer",
            accessToken = accessToken,
            refreshToken = refreshToken
        )
    }

    fun verifyJwt() = JWT
        .require(Algorithm.HMAC256(config.security.secret))
        .withAudience(config.security.audience)
        .withIssuer(config.security.issuer)
        .build()

    suspend fun blacklistToken(token: String) {
        val decodedJwt = JWT.decode(token)
        val ttl = decodedJwt.expiresAt.time - System.currentTimeMillis()
        if (ttl > 0) {
            redis.setex("blacklist:$token", ttl / 1000, "true")
        }
    }

    suspend fun isTokenBlacklisted(token: String): Boolean {
        return redis.get("blacklist:$token") != null
    }

    private fun generateToken(
        tokenType: String, // access and refresh
        additionalClaims: Map<String, Any> = emptyMap()
    ): String {

        val expiry = if (tokenType == "access") ACCESS_TOKEN_EXPIRY else REFRESH_TOKEN_EXPIRY

        return JWT.create()
            .withAudience(config.security.audience)
            .withSubject(additionalClaims["userId"].toString())
            .withIssuer(config.security.issuer)
            .withClaim("email", additionalClaims["email"].toString())
            .withClaim("tokenType", tokenType)
            .withIssuedAt(Date())
            .withJWTId(UUID.randomUUID().toString())
            .withExpiresAt(Date(System.currentTimeMillis() + expiry))
            .apply {
                additionalClaims.forEach { (key, value) ->
                    when (value) {
                        is String -> withClaim(key, value)
                        is Int -> withClaim(key, value)
                        is Boolean -> withClaim(key, value)
                        is Date -> withClaim(key, value)
                    }
                }
            }
            .sign(Algorithm.HMAC256(config.security.secret))
    }

}

// Custom JWT principal with your claims
data class CustomJwtPrincipal(
    val userId: String,
    val email: String,
    val jti: String,
    val expiresAt: Date,
    val name: String = userId
)