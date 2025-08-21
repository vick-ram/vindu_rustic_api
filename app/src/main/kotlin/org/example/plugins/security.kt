package org.example.plugins

import com.google.gson.GsonBuilder
import io.ktor.client.HttpClient
import io.ktor.http.HttpMethod
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.OAuthServerSettings
import io.ktor.server.auth.UnauthorizedResponse
import io.ktor.server.auth.authentication
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.auth.oauth
import io.ktor.server.auth.session
import io.ktor.server.response.respond
import io.ktor.server.sessions.Sessions
import io.ktor.server.sessions.cookie
import org.example.domain.models.CartItem
import org.example.utils.CustomJwtPrincipal
import org.example.utils.GsonSessionSerializer
import org.example.utils.RedisSessionStorage
import org.example.utils.isTokenBlacklisted
import org.example.utils.verifyJwt
import java.util.Date

data class AuthSession(
    val userId: String,
    val email: String,
    val lastAccess: Long = System.currentTimeMillis()
)

data class CartSession(
    val items: MutableList<CartItem> = mutableListOf(),
    val lastUpdated: Long = System.currentTimeMillis()
)

fun Application.configureSecurity(
    realm: String,
    secret: String,
    issuer: String,
    audience: String,
    httpClient: HttpClient,
    clientID: String,
    clientSecret: String
) {
    val sessionStorage = RedisSessionStorage()
    val environment = this.developmentMode
    val gson = GsonBuilder().create()
    val redirects = mutableMapOf<String, String>()

    install(Sessions) {
        cookie<AuthSession>("auth_session", storage = sessionStorage) {
            cookie.extensions["SameSite"] = "lax"
            cookie.path = "/"
            cookie.maxAgeInSeconds = 3600 * 24 * 1 // A day
            cookie.secure = environment
            serializer = GsonSessionSerializer(gson, AuthSession::class.java)
            transform(SessionTransportTransformerEncrypt())
        }

        cookie<CartSession>("cart_session") {
            cookie.extensions["SameSite"] = "lax"
            cookie.path = "/"
            cookie.maxAgeInSeconds = 3600 * 24 * 30 // A week
            serializer = GsonSessionSerializer(gson, CartSession::class.java)
//            transform(SessionTransportTransformerEncrypt())
        }
    }
    authentication {
//        Session Authentication
        session<AuthSession>("auth-session") {
            validate { userSession ->
                if (userSession.userId.isNotBlank() && System.currentTimeMillis() - userSession.lastAccess < 3600_000) {
                    userSession.copy(lastAccess = System.currentTimeMillis())
                } else {
                    null
                }
            }
            challenge {
                call.respond(UnauthorizedResponse())
            }
        }

//        JWT authentication
        jwt("auth-jwt") {
            this.realm = realm
            verifyJwt(secret = secret, issuer = issuer, audience = audience)?.let {
                verifier(
                    it
                )
            }
            validate { credential ->
                if (isTokenBlacklisted(credential.payload.id)) {
                    return@validate null
                }

                // Check expiration
                if (credential.payload.expiresAt.before(Date())) {
                    return@validate null
                }

                return@validate CustomJwtPrincipal(
                    userId = credential.payload.subject,
                    email = credential.payload.getClaim("email").asString(),
                    jti = credential.payload.id,
                    expiresAt = credential.payload.expiresAt,
                )
            }

            challenge { _, _ ->
                throw AuthenticationException("Token is not valid or has expired")
            }
        }

        oauth("auth-oauth-google") {
            urlProvider = { "http://localhost:8000/users/auth/callback" }
            providerLookup = {
                OAuthServerSettings.OAuth2ServerSettings(
                    name = "google",
                    authorizeUrl = "https://accounts.google.com/o/oauth2/auth",
                    accessTokenUrl = "https://accounts.google.com/o/oauth2/token",
                    requestMethod = HttpMethod.Post,
                    clientId = clientID,
                    clientSecret = clientSecret,
                    defaultScopes = listOf("https://www.googleapis.com/auth/userinfo.profile"),
                    extraAuthParameters = listOf("access_type" to "offline"),
                    onStateCreated = { call, state ->
                        call.request.queryParameters["redirectUrl"]?.let {
                            redirects[state] = it
                        }
                    }
                )
            }
            client = httpClient
        }
    }
}
