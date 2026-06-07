package org.example.config

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions
import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.server.websocket.webSocket
import io.ktor.util.*
import io.ktor.websocket.DefaultWebSocketSession
import org.example.plugins.AuthSession
import org.example.plugins.AuthenticationException

enum class AuthType { NONE, SESSION, JWT, ANY }

enum class HttpMethodType { GET, POST, PUT, PATCH, DELETE, HEAD, OPTIONS }

enum class EndpointType { HTTP, WEBSOCKET }

data class DynamicRouteConfig(
    val path: String,
    val methods: Set<HttpMethodType> = emptySet(),
    val handler: RoutingHandler,
    val name: String? = null,
    val version: String? = null,
    val requiresAuth: Boolean = false,
    val authType: AuthType = AuthType.NONE,
    val metadata: Map<String, Any> = emptyMap(),
    val endpointType: EndpointType = EndpointType.HTTP,
    val websocketHandler: WebsocketHandler? = null
)

typealias WebsocketHandler = suspend DefaultWebSocketServerSession.() -> Unit

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

    // Register a  HTTP route
    fun addRoute(config: DynamicRouteConfig) {
        validateRoute(config)
        dynamicRoutes.add(config)
    }

    // Register a websocket route
    fun addWebsocketRoute(
        path: String,
        handler: WebsocketHandler,
        name: String? = null,
        version: String? = null,
        requiresAuth: Boolean = false,
        authType: AuthType = AuthType.NONE,
        metadata: Map<String, Any> = emptyMap()
    ) {
        val config = DynamicRouteConfig(
            path = path,
            handler = {},
            name = name,
            version = version,
            requiresAuth = requiresAuth,
            authType = authType,
            metadata = metadata,
            endpointType = EndpointType.WEBSOCKET,
            websocketHandler = handler
        )
        validateRoute(config)
        dynamicRoutes.add(config)
    }

    // Register multiple routes
    fun addRoutes(configs: List<DynamicRouteConfig>) {
//        configs.forEach { validateRoute(it) }
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
//            validateRoute(enhancedConfig)
            dynamicRoutes.add(enhancedConfig)
        }
    }

    fun registerRoutes(routing: Routing) {
        dynamicRoutes.forEach { config ->
            when (config.endpointType) {
                EndpointType.HTTP -> registerHttpRoute(config, routing)
                EndpointType.WEBSOCKET -> registerWebSocketRoute(config, routing)
            }
        }
    }

    private fun registerHttpRoute(config: DynamicRouteConfig, routing: Routing) {
        authenticateIfNeeded(config, routing) {
            val handler = wrapHandler(config)
            config.methods.forEach { method ->
                when (method) {
                    HttpMethodType.GET -> get(config.path, handler)
                    HttpMethodType.POST -> post(config.path, handler)
                    HttpMethodType.PUT -> put(config.path, handler)
                    HttpMethodType.PATCH -> patch(config.path, handler)
                    HttpMethodType.DELETE -> delete(config.path, handler)
                    HttpMethodType.HEAD -> head(config.path, handler)
                    HttpMethodType.OPTIONS -> options(config.path, handler)
                }.apply { applyRouteMetadata(config) }
            }
        }
    }

    private fun registerWebSocketRoute(config: DynamicRouteConfig, routing: Routing) {
        authenticateIfNeeded(config, routing) {
            config.websocketHandler?.let {
                webSocket(config.path, it).apply {
                    applyRouteMetadata(config)
                }
            }
        }
    }

    private fun Route.applyRouteMetadata(config: DynamicRouteConfig) {
        attributes.put(RouteMetadataKey, config)
    }

    fun findRoute(path: String, method: HttpMethodType? = null): DynamicRouteConfig? {
        return dynamicRoutes.find { config ->
            when (config.endpointType) {
                EndpointType.HTTP -> method != null && config.methods.contains(method) && pathMatchesRoute(path, config.path)
                EndpointType.WEBSOCKET -> method == null && pathMatchesRoute(path, config.path)
            }
        }
    }

    fun getAllRoutes(): List<DynamicRouteConfig> = dynamicRoutes.toList()

    fun getHttpRoutes(): List<DynamicRouteConfig> =
        dynamicRoutes.filter { it.endpointType == EndpointType.HTTP }

    fun getWebSocketRoutes(): List<DynamicRouteConfig> =
        dynamicRoutes.filter { it.endpointType == EndpointType.WEBSOCKET }

    private fun pathMatchesRoute(requestPath: String, routePath: String): Boolean {
        val regexPattern = routePath
            .replace(Regex("\\{.*?}"), "[^/]+") // Convert {param} to wildcard
            .let { $$"^$$it$" }

        return Regex(regexPattern).matches(requestPath)
    }

    private fun wrapHandler(config: DynamicRouteConfig): RoutingHandler {
        return handler@{
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

    private fun authenticateIfNeeded(
        config: DynamicRouteConfig,
        routing: Routing,
        builder: Route.() -> Unit
    ) {
        when (config.authType) {
            AuthType.JWT -> routing.authenticate("auth-jwt") { builder() }
            AuthType.SESSION -> routing.route("") { builder() }
            AuthType.ANY -> routing.authenticate("auth-jwt", "session") { builder() }
            else -> routing.route("") { builder() }
        }
    }

    private fun validateRoute(config: DynamicRouteConfig) {
        when (config.endpointType) {
            EndpointType.HTTP -> {
                if (config.methods.isEmpty()) {
                    throw IllegalArgumentException("HTTP routes must specify at least one method")
                }
                val conflict = dynamicRoutes.find { existing ->
                    existing.endpointType == EndpointType.HTTP &&
                            existing.path == config.path &&
                            existing.methods.any { it in config.methods }
                }
                if (conflict != null) {
                    throw IllegalArgumentException("Route conflict: ${config.path} with methods ${config.methods}")
                }
            }
            EndpointType.WEBSOCKET -> {
                if (config.websocketHandler == null) {
                    throw IllegalArgumentException("WebSocket routes must have a websocketHandler")
                }
                val conflict = dynamicRoutes.find { existing ->
                    existing.endpointType == EndpointType.WEBSOCKET && existing.path == config.path
                }
                if (conflict != null) {
                    throw IllegalArgumentException("WebSocket route conflict: ${config.path}")
                }
            }
        }
    }

//    fun printRoutes() {
//        println("=== Registered Routes ===")
//        if (dynamicRoutes.isEmpty()) {
//            println("No routes registered")
//            return
//        }
//        getAllRoutes().forEach { route ->
//            println("${route.methods.joinToString()} ${route.path} [Auth=${route.requiresAuth}]")
//        }
//    }

    fun printRoutes() {
        println("=== Registered Routes ===")
        if (dynamicRoutes.isEmpty()) {
            println("No routes registered")
            return
        }

        val httpRoutes = getHttpRoutes()
        val webSocketRoutes = getWebSocketRoutes()

        if (httpRoutes.isNotEmpty()) {
            println("\n--- HTTP Routes ---")
            httpRoutes.forEach { route ->
                println("${route.methods.joinToString()} ${route.path} [Auth=${route.requiresAuth}]")
            }
        }

        if (webSocketRoutes.isNotEmpty()) {
            println("\n--- WebSocket Routes ---")
            webSocketRoutes.forEach { route ->
                println("WS     ${route.path} [Auth=${route.requiresAuth}]")
            }
        }
    }
}


