package org.example.utils

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions
import io.ktor.util.*
import org.example.plugins.AuthSession

enum class AuthType { NONE, SESSION, JWT, ANY }

enum class HttpMethodType { GET, POST, PUT, PATCH, DELETE, HEAD, OPTIONS }

data class DynamicRouteConfig(
    val path: String,
    val methods: Set<HttpMethodType>,
    val handler: suspend RoutingContext.() -> Unit,
    val name: String? = null,
    val version: String? = null,
    val requiresAuth: Boolean = false,
    val authType: AuthType = AuthType.NONE,
    val metadata: Map<String, Any> = emptyMap()
)

data class RouteGroupConfig(
    val prefix: String,
    val requiresAuth: Boolean = false,
    val authType: AuthType = AuthType.NONE,
    val version: String? = null,
    val metadata: Map<String, Any> = emptyMap()
)

class DynamicRouteFactory {

    companion object {
        val RouteMetadataKey = AttributeKey<DynamicRouteConfig>("RouteMetadata")
    }

    private val dynamicRoutes = mutableListOf<DynamicRouteConfig>()
    private val routeGroups = mutableMapOf<String, RouteGroupConfig>()
    private val routeHandlers = mutableMapOf<String, suspend RoutingContext.() -> Unit>()

    // Register a single route
    fun addRoute(config: DynamicRouteConfig) {
        val conflict = dynamicRoutes.find { existing ->
            existing.path == config.path && existing.methods.any { it in config.methods }
        }

        if (conflict != null) {
            throw IllegalArgumentException("Route conflict: ${config.path} with methods ${config.methods}")
        }

        dynamicRoutes.add(config)
    }

    // Register multiple routes
    fun addRoutes(configs: List<DynamicRouteConfig>) {
        dynamicRoutes.addAll(configs)
    }

    // Register a route group
    fun registerGroup(name: String, config: RouteGroupConfig, routes: List<DynamicRouteConfig>) {
        routeGroups[name] = config
        routes.forEach { route ->
            val fullPath = buildString {
                if (config.prefix.isNotEmpty()) append(config.prefix)
                if (!route.version.isNullOrEmpty()) append("/v${route.version}")
                append(route.path)
            }

            val enhancedConfig = route.copy(
                path = fullPath,
                requiresAuth = config.requiresAuth || route.requiresAuth,
                version = route.version ?: config.version
            )

            dynamicRoutes.add(enhancedConfig)
        }
    }

    // Register handler that can be referenced by ID
    fun registerHandler(handlerId: String, handler: suspend RoutingContext.() -> Unit) {
        routeHandlers[handlerId] = handler
    }

    // Get handler by ID (useful for DI)
    fun getHandler(handlerId: String): (suspend RoutingContext.() -> Unit)? {
        return routeHandlers[handlerId]
    }

    fun registerRoutes(routing: Routing) {
        dynamicRoutes.forEach { config ->
            val wrapHandler = wrapHandler(config)
            config.methods.forEach { method ->
                when (method) {
                    HttpMethodType.GET -> routing.get(config.path, wrapHandler).apply { applyRouteMetadata(config) }
                    HttpMethodType.POST -> routing.post(config.path, wrapHandler).apply { applyRouteMetadata(config) }
                    HttpMethodType.PUT -> routing.put(config.path, wrapHandler).apply { applyRouteMetadata(config) }
                    HttpMethodType.PATCH -> routing.patch(config.path, wrapHandler).apply { applyRouteMetadata(config) }
                    HttpMethodType.DELETE -> routing.delete(config.path, wrapHandler).apply { applyRouteMetadata(config) }
                    HttpMethodType.HEAD -> routing.head(config.path, wrapHandler).apply { applyRouteMetadata(config) }
                    HttpMethodType.OPTIONS -> routing.options(config.path, wrapHandler).apply { applyRouteMetadata(config) }
                }
            }
        }
    }

    private fun Route.applyRouteMetadata(config: DynamicRouteConfig) {
        attributes.put(RouteMetadataKey, config)
    }

    fun findRoute(path: String, method: HttpMethodType): DynamicRouteConfig? {
        return dynamicRoutes.find { config ->
            config.methods.contains(method) && pathMatchesRoute(path, config.path)
        }
    }

    fun getAllRoutes(): List<DynamicRouteConfig> =dynamicRoutes.toList()

    private fun pathMatchesRoute(requestPath: String, routePath: String): Boolean {
        val regexPattern = routePath
            .replace(Regex("\\{.*?}"), "[^/]+") // Convert {param} to wildcard
            .let { $$"^$$it$" }

        return Regex(regexPattern).matches(requestPath)
    }

    private fun wrapHandler(config: DynamicRouteConfig): suspend RoutingContext.() -> Unit {
        return handler@ {
            if (config.requiresAuth) {
                when (config.authType) {
                    AuthType.SESSION -> {
                        val session = call.sessions.get<AuthSession>()
                        if (session == null) {
                            call.respond(HttpStatusCode.Unauthorized, "Session required")
                            return@handler
                        }
                    }
                    AuthType.JWT -> {
                        val principal = call.principal<JWTPrincipal>()
                        if (principal == null) {
                            call.respond(HttpStatusCode.Unauthorized, "JWT required")
                            return@handler
                        }
                    }
                    AuthType.ANY -> {
                        val session = call.sessions.get<AuthSession>()
                        val principal = call.principal<JWTPrincipal>()
                        if (session == null && principal == null) {
                            call.respond(HttpStatusCode.Unauthorized, "Auth required")
                            return@handler
                        }
                    }
                    else -> {}
                }
            }
            config.handler(this)
        }
    }
}


