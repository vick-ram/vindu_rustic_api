package org.example.utils

import io.ktor.server.routing.*
import io.ktor.util.*

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
    val requiredPermissions: Set<String> = emptySet(),
    val metadata: Map<String, Any> = emptyMap()
)

data class RouteGroupConfig(
    val prefix: String,
    val requiresAuth: Boolean = false,
    val authType: AuthType = AuthType.NONE,
    val commonPermissions: Set<String> = emptySet(),
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
            val fullPath = if (config.prefix.isNotEmpty()) {
                "${config.prefix}${route.path}"
            } else {
                route.path
            }

            val enhancedConfig = route.copy(
                path = fullPath,
                requiresAuth = config.requiresAuth || route.requiresAuth,
                requiredPermissions = config.commonPermissions + route.requiredPermissions,
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
            config.methods.forEach { method ->
                when (method) {
                    HttpMethodType.GET -> routing.get(config.path, config.handler).apply { applyRouteMetadata(config) }
                    HttpMethodType.POST -> routing.post(config.path, config.handler).apply { applyRouteMetadata(config) }
                    HttpMethodType.PUT -> routing.put(config.path, config.handler).apply { applyRouteMetadata(config) }
                    HttpMethodType.PATCH -> routing.patch(config.path, config.handler).apply { applyRouteMetadata(config) }
                    HttpMethodType.DELETE -> routing.delete(config.path, config.handler).apply { applyRouteMetadata(config) }
                    HttpMethodType.HEAD -> routing.head(config.path, config.handler).apply { applyRouteMetadata(config) }
                    HttpMethodType.OPTIONS -> routing.options(config.path, config.handler).apply { applyRouteMetadata(config) }
                }
            }
        }
    }

    private fun Route.applyRouteMetadata(config: DynamicRouteConfig) {
        attributes.put(RouteMetadataKey, config)
    }

    fun findRoute(path: String): DynamicRouteConfig? {
        return dynamicRoutes.find { it.path == path }
    }

    fun getAllRoutes(): List<DynamicRouteConfig> =dynamicRoutes.toList()

}
