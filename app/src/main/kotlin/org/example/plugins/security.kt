package org.example.plugins

import com.google.gson.Gson
import io.ktor.client.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.sessions.*
import io.ktor.util.*
import org.example.config.AppConfig
import org.example.config.ApplicationPlugin
import org.example.domain.models.sales.CartItem
import org.example.services.CustomJwtPrincipal
import org.example.utils.*
import org.koin.ktor.ext.inject
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

object SecurityModule : ApplicationPlugin {
    override fun install(application: Application) {
        val sessionStorage = RedisSessionStorage()
        val config = AppConfig.load(application)
        val redirects = mutableMapOf<String, String>()
        val secretSignKey = hex(config.security.secretSignKey)
        val secretEncryptionKey = hex(config.security.secretEncryptionKey)

        application.install(Sessions) {
            val gson by application.inject<Gson>()
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
                cookie.secure = config.server.development
                cookie.maxAgeInSeconds = 3600 * 24 * 30 // A week
                serializer = GsonSessionSerializer(gson, CartSession::class.java)
                transform(SessionTransportTransformerEncrypt(secretEncryptionKey, secretSignKey))
            }
        }

        application.authentication {
            val httpClient by application.inject<HttpClient>()
//        Session Authentication
            session<AuthSession>("auth-session") {
                validate { it.takeIf { session -> session.userId.isNotBlank() } }
                challenge {
                    call.respond(UnauthorizedResponse())
                }
            }

//        JWT authentication
            jwt("auth-jwt") {
                realm = config.security.realm
                verifyJwt(secret = config.security.secret, issuer = config.security.issuer, audience = config.security.audience)?.let {
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
                        clientId = config.security.clientID,
                        clientSecret = config.security.clientSecret,
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
}
