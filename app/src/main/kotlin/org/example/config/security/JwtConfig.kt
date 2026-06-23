package org.example.config.security

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import org.example.config.SecurityConfig
import java.security.SecureRandom
import java.util.Date
import java.util.UUID

class JwtConfig(private val config: SecurityConfig) {
    val verifier: JWTVerifier = JWT
        .require(Algorithm.HMAC256(config.secret))
        .withIssuer(config.issuer)
        .withAudience(config.audience)
        .build()

    fun generateAccessToken(userId: String, email: String): String {
        return JWT.create()
            .withIssuer(config.issuer)
            .withAudience(config.audience)
            .withClaim("userId", userId)
            .withClaim("email", email)
            .withExpiresAt(Date(System.currentTimeMillis() + config.accessExpiry))
            .sign(Algorithm.HMAC256(config.secret))
    }

    fun generateRefreshToken(userId: String): String {
        return UUID.randomUUID().toString() + "." + SecureRandom().nextLong()
    }
}