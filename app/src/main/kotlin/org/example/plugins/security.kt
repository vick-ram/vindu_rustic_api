package org.example.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.forwardedheaders.*
import io.ktor.server.plugins.hsts.*
import io.ktor.server.response.*
import io.ktor.server.sessions.*
import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands
import kotlinx.serialization.Serializable
import org.example.config.AppConfig
import org.example.config.ApplicationPlugin
import org.example.config.security.JwtConfig
import org.example.utils.RedisSessionStorage
import org.koin.ktor.ext.inject

object SecurityModule : ApplicationPlugin {
    @OptIn(ExperimentalLettuceCoroutinesApi::class)
    override fun install(application: Application) {
        val jwtConfig by application.inject<JwtConfig>()
        val redisCommands by application.inject<RedisCoroutinesCommands<String, String>>()
        val sessionStorage = RedisSessionStorage(redisCommands)
        val config = AppConfig.load(application)
        val redirects = mutableMapOf<String, String>()
        val secretSignKey = config.security.secretSignKey.hexToByteArray()
        val secretEncryptionKey = config.security.secretEncryptionKey.hexToByteArray()

        application.install(XForwardedHeaders)

        application.install(HSTS) {
            maxAgeInSeconds = 365 * 24 * 60 * 50
            includeSubDomains = true
            preload = true
        }

        application.install(CORS) {
            config.cors.allowedMethods.forEach { method ->
                allowMethod(HttpMethod.parse(method))
            }
            config.cors.allowedHeaders.forEach { header ->
                allowHeader(header)
            }
            allowCredentials = config.cors.allowCredentials
            allowNonSimpleContentTypes = true
            maxAgeInSeconds = config.cors.maxAgeSeconds
            anyHost() // @TODO: Don't do this in production if possible. Try to limit it.
        }

        application.install(Sessions) {
            cookie<AuthSession>("auth_session") {
                cookie.path = "/"
                cookie.maxAgeInSeconds = 7 * 24 * 60 * 60
                cookie.httpOnly = true
                cookie.secure = true
                cookie.sameSite = SameSite.Lax
            }
        }

        application.authentication {
            jwt("auth-jwt") {
                verifier(jwtConfig.verifier)
                validate { credential ->
                    if (credential.payload.getClaim("userId").asString() != null) {
                        JWTPrincipal(credential.payload)
                    } else null
                }
                challenge { _, _ ->
                    call.respond(HttpStatusCode.Unauthorized)
                }
            }
            session<AuthSession>("auth-session") {
                validate { session ->
                    if (session.userId.isNotEmpty()) {
                        session
                    } else null
                }
                challenge { _ ->
                    call.respond(HttpStatusCode.Unauthorized)
                }
            }
        }
    }
}

@Serializable
data class AuthSession(
    val userId: String,
    val email: String
)