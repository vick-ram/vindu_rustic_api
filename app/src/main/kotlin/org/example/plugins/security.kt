package org.example.plugins

import io.ktor.client.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.sessions.*
import io.ktor.util.*
import org.example.domain.models.CartItem
import org.example.utils.*
import java.math.BigDecimal
import java.util.*
import kotlin.uuid.ExperimentalUuidApi

data class AuthSession(
    val userId: String,
    val email: String,
    val lastAccess: Long = System.currentTimeMillis()
)

data class CartSession @OptIn(ExperimentalUuidApi::class) constructor(
    val sessionId: String = shortUUID(),
    val userId: String? = null,
    val items: MutableList<CartItem> = mutableListOf(),
    val lastUpdated: Long = System.currentTimeMillis(),
    var total: BigDecimal = BigDecimal.ZERO
) {
    fun calculateTotal() {
        total = items.sumOf { it.total }
    }

    fun addItem(newItem: CartItem) {
        val existingItem = items.find { it.productId == newItem.productId }
        if (existingItem != null) {
            existingItem.quantity += newItem.quantity
        } else {
            items.add(newItem)
        }
        calculateTotal()
    }

    fun removeItem(productId: String) {
        items.removeAll { it.productId == productId }
        calculateTotal()
    }

    fun updateQuantity(productId: String, quantity: Int) {
        items.find { it.productId == productId }?.quantity = quantity
        calculateTotal()
    }
}

fun Application.configureSecurity(config: SecurityConfig, httpClient: HttpClient) {
    val sessionStorage = RedisSessionStorage()
    val environment = this.developmentMode
    val gson = GsonFactory.gson
    val redirects = mutableMapOf<String, String>()
    val secretEncryptionKey = hex("153f6cb438af8a004c4c9c4fa6f5cc00")
    val secretSignKey = hex("de38a5bdefe18c1d64ce10d8b0610bff")

    install(Sessions) {
        cookie<AuthSession>("auth-session", storage = sessionStorage) {
            cookie.extensions["SameSite"] = "lax"
            cookie.path = "/"
            cookie.maxAgeInSeconds = 7 * 24 * 60 * 60 // A week
            cookie.httpOnly = true
//            cookie.secure = environment
            serializer = GsonSessionSerializer(gson, AuthSession::class.java)
            transform(SessionTransportTransformerEncrypt(secretEncryptionKey, secretSignKey))
        }

        cookie<CartSession>("cart_session") {
            cookie.extensions["SameSite"] = "lax"
            cookie.path = "/"
            cookie.secure = environment
            cookie.maxAgeInSeconds = 3600 * 24 * 30 // A week
            serializer = GsonSessionSerializer(gson, CartSession::class.java)
            transform(SessionTransportTransformerEncrypt(secretEncryptionKey, secretSignKey))
        }
    }
    authentication {
//        Session Authentication
        session<AuthSession>("auth-session") {
            validate { it.takeIf { session -> session.userId.isNotBlank() } }
            challenge {
                call.respond(UnauthorizedResponse())
            }
        }

//        JWT authentication
        jwt("auth-jwt") {
            realm = config.realm
            verifyJwt(secret = config.secret, issuer = config.issuer, audience = config.audience)?.let {
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
                    clientId = config.clientID,
                    clientSecret = config.clientSecret,
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
