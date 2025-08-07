package org.example.utils

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.auth.Principal
import io.ktor.server.auth.jwt.JWTPayloadHolder
import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import java.util.Date
import java.util.UUID

fun makeJwtToken(
    issuer: String,
    audience: String,
    secret: String,
    email: String,
    userId: String,
    additionalClaims: Map<String, Any> = emptyMap()
): String? = JWT.create()
    .withAudience(audience)
    .withSubject(userId)
    .withIssuer(issuer)
    .withClaim("email", email)
    .withIssuedAt(Date())
    .withJWTId(UUID.randomUUID().toString())
    .withExpiresAt(Date(System.currentTimeMillis() + 60 * 1000))
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
    .sign(Algorithm.HMAC256(secret))

fun verifyJwt(
    secret: String,
    audience: String,
    issuer: String
): JWTVerifier? = JWT
    .require(Algorithm.HMAC256(secret))
    .withAudience(audience)
    .withIssuer(issuer)
    .build()

@OptIn(ExperimentalLettuceCoroutinesApi::class)
suspend fun blacklistToken(token: String) {
    val decodedJwt = JWT.decode(token)
    val ttl = decodedJwt.expiresAt.time - System.currentTimeMillis()
    if (ttl > 0) {
        RedisService.commands.setex(
            "blacklist:$token",
            ttl / 1000,
            "true"
        )
    }
}

@OptIn(ExperimentalLettuceCoroutinesApi::class)
suspend fun isTokenBlacklisted(token: String): Boolean {
    return RedisService.commands.get("blacklist:$token") != null
}

// Custom JWT principal with your claims
data class CustomJwtPrincipal(
    val userId: String,
    val email: String,
    val jti: String,
    val expiresAt: Date,
    val name: String = userId
): Principal
