package org.example.config.security

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import org.example.config.AppConfig
import org.example.di.Component
import org.example.di.Inject
import java.util.*

@Component
class JwtConfig @Inject constructor(appConfig: AppConfig) {
    private val config = appConfig.security

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
        val randomPart = UUID.randomUUID().toString().replace("-", "")
        return "$userId.$randomPart"
    }
}
