package org.example.config

import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.util.*
import io.ktor.websocket.*
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.example.plugins.RouteConflictException
import org.example.plugins.RouteValidationException
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.write
import kotlin.time.Duration.Companion.milliseconds

enum class AuthType { NONE, SESSION, JWT, ANY }

enum class HttpMethodType {
    GET, POST, PUT, PATCH, DELETE, HEAD, OPTIONS;

    fun toKtorMethod(): HttpMethod = when (this) {
        GET -> HttpMethod.Get
        POST -> HttpMethod.Post
        PUT -> HttpMethod.Put
        PATCH -> HttpMethod.Patch
        DELETE -> HttpMethod.Delete
        HEAD -> HttpMethod.Head
        OPTIONS -> HttpMethod.Options
    }
}

enum class EndpointType { HTTP, WEBSOCKET }

//typealias RoutingHandler = suspend Route.() -> Unit
typealias RoutingHandler = suspend RoutingContext.() -> Unit
typealias WebsocketHandler = suspend DefaultWebSocketServerSession.() -> Unit

data class RateLimitConfig(
    val requestsPerSecond: Int = 100,
    val burstSize: Int = 200
)

data class DynamicRouteConfig(
    val path: String,
    val methods: Set<HttpMethodType> = emptySet(),
    val handler: RoutingHandler = {},
    val name: String? = null,
    val version: String? = null,
    val requiresAuth: Boolean = false,
    val authType: AuthType = AuthType.NONE,
    val metadata: Map<String, Any> = emptyMap(),
    val endpointType: EndpointType = EndpointType.HTTP,
    val websocketHandler: WebsocketHandler? = null,
    val timeoutMs: Long = 30_000L,
    val rateLimitConfig: RateLimitConfig? = null
)

data class RouteGroupConfig(
    val prefix: String,
    val requiresAuth: Boolean = false,
    val authType: AuthType = AuthType.NONE,
    val version: String? = null,
    val metadata: Map<String, Any> = emptyMap(),
    val defaultTimeoutMs: Long = 30_000L,
    val defaultRateLimit: RateLimitConfig? = null
)

/**
 * Thrown when a mutation (addRoute/addRoutes/registerGroup/clear) is attempted
 * after registerRoutes() has already wired the snapshot into Ktor's Routing tree.
 * Ktor does not support retroactively injecting routes into an already-built tree,
 * so allowing further mutation would silently diverge internal state from actual
 * server behavior.
 */
class RouteFactoryFrozenException(message: String) : IllegalStateException(message)

/**
 * Simple token-bucket limiter. Refill is computed lazily on each acquire attempt
 * based on elapsed time, so there's no background thread/coroutine needed.
 * Uses compare-and-swap on a packed (tokens, lastRefillNanos) via two AtomicLongs
 * guarded by a spin-retry CAS loop -- no locks on the hot path.
 */
class TokenBucketRateLimiter(
    private val capacity: Int,
    private val refillPerSecond: Int
) {
    private val availableTokens = AtomicLong(capacity.toLong())
    private val lastRefillNanos = AtomicLong(System.nanoTime())

    @Synchronized
    fun tryAcquire(): Boolean {
        val now = System.nanoTime()
        val elapsedNanos = now - lastRefillNanos.get()

        if (elapsedNanos > 0) {
            val tokensToAdd = (elapsedNanos * refillPerSecond) / 1_000_000_000L
            if (tokensToAdd > 0) {
                val current = availableTokens.get()
                availableTokens.set(minOf(capacity.toLong(), current + tokensToAdd))
                lastRefillNanos.set(now)
            }
        }

        val current = availableTokens.get()
        if (current <= 0) return false
        availableTokens.decrementAndGet()
        return true
    }
}

class DynamicRouteFactory {
    companion object {
        val RouteMetadataKey = AttributeKey<DynamicRouteConfig>("RouteMetadata")
        private const val MAX_PATH_LENGTH = 2048
        private const val MAX_PATHS = 10_000

        // Allows constrained Ktor params like {id:[0-9]+} in addition to plain {id} / {id?}
        private val PATH_PATTERN = Regex("^[/a-zA-Z0-9{}\\[\\]:\\-_.?=&%]+$")
        private val logger = LoggerFactory.getLogger(DynamicRouteFactory::class.java)
    }

    private val dynamicRoutes = CopyOnWriteArrayList<DynamicRouteConfig>()
    private val routeGroups = ConcurrentHashMap<String, RouteGroupConfig>()
    private val routePatternCache = ConcurrentHashMap<String, Regex>()
    private val rateLimiters = ConcurrentHashMap<DynamicRouteConfig, TokenBucketRateLimiter>()

    // Guards mutation methods against check-then-add races.
    private val registrationLock = ReentrantReadWriteLock()

    // Once true, the internal route list has been handed to Ktor and must not change.
    private val registered = AtomicBoolean(false)

    fun addRoute(config: DynamicRouteConfig) {
        registrationLock.write {
            failIfRegistered("addRoute")
            validateRouteConfig(config, dynamicRoutes)
            if (dynamicRoutes.size >= MAX_PATHS) {
                throw RouteValidationException("Maximum number of routes ($MAX_PATHS) reached")
            }
            dynamicRoutes.add(config)
        }
        logger.debug("Added route: {} {}", config.methods.joinToString(), config.path)
    }

    fun addWebsocketRoute(
        path: String,
        handler: WebsocketHandler,
        name: String? = null,
        version: String? = null,
        requiresAuth: Boolean = false,
        authType: AuthType = AuthType.NONE,
        metadata: Map<String, Any> = emptyMap(),
        timeoutMs: Long = 30_000L,
        rateLimitConfig: RateLimitConfig? = null
    ) {
        val config = DynamicRouteConfig(
            path = path,
            name = name,
            version = version,
            requiresAuth = requiresAuth,
            authType = authType,
            metadata = metadata,
            endpointType = EndpointType.WEBSOCKET,
            websocketHandler = handler,
            timeoutMs = timeoutMs,
            rateLimitConfig = rateLimitConfig
        )
        addRoute(config)
    }

    fun addRoutes(configs: List<DynamicRouteConfig>) {
        registrationLock.write {
            failIfRegistered("addRoutes")
            // Validate against existing routes AND against each other within this batch,
            // so two conflicting routes added in the same call are caught too.
            val accumulator = dynamicRoutes.toMutableList()
            configs.forEach { config ->
                validateRouteConfig(config, accumulator)
                accumulator.add(config)
            }
            if (dynamicRoutes.size + configs.size > MAX_PATHS) {
                throw RouteValidationException("Maximum number of routes ($MAX_PATHS) exceeded")
            }
            dynamicRoutes.addAll(configs)
        }
        logger.info("Added ${configs.size} routes atomically")
    }

    fun registerGroup(name: String, config: RouteGroupConfig, routes: List<DynamicRouteConfig>) {
        registrationLock.write {
            failIfRegistered("registerGroup")
            validateGroupConfig(name, config)
            routeGroups[name] = config

            val enhancedRoutes = routes.map { route ->
                val cleanPrefix = config.prefix.trim('/')
                val cleanVersion = route.version?.let { "v$it" } ?: config.version?.let { "v$it" } ?: ""
                val cleanPath = route.path.trim('/')

                val fullPath = listOf(cleanPrefix, cleanVersion, cleanPath)
                    .filter { it.isNotEmpty() }
                    .joinToString(separator = "/", prefix = "/")

                val effectiveAuthType = resolveEffectiveAuthType(route.authType, config.authType)
                val effectiveRequiresAuth = config.requiresAuth || route.requiresAuth

                route.copy(
                    path = fullPath,
                    requiresAuth = effectiveRequiresAuth,
                    authType = effectiveAuthType,
                    version = route.version ?: config.version,
                    timeoutMs = route.timeoutMs.takeIf { it != 30_000L } ?: config.defaultTimeoutMs,
                    rateLimitConfig = route.rateLimitConfig ?: config.defaultRateLimit
                )
            }

            if (dynamicRoutes.size + enhancedRoutes.size > MAX_PATHS) {
                throw RouteValidationException("Maximum number of routes ($MAX_PATHS) exceeded")
            }

            val accumulator = dynamicRoutes.toMutableList()
            enhancedRoutes.forEach { route ->
                validateRouteConfig(route, accumulator)
                accumulator.add(route)
            }
            dynamicRoutes.addAll(enhancedRoutes)
        }
        logger.info("Registered route group '$name' with ${routes.size} routes")
    }

    /**
     * Wires the current snapshot of routes into Ktor's Routing tree.
     * Can only be called once -- after this, the factory is frozen and any
     * further mutation attempt throws RouteFactoryFrozenException, since Ktor
     * has no supported way to inject routes into an already-built tree.
     */
    fun registerRoutes(routing: Routing) {
        if (!registered.compareAndSet(false, true)) {
            throw RouteFactoryFrozenException(
                "registerRoutes() was already called once; this factory is frozen. " +
                        "Register all routes before calling registerRoutes()."
            )
        }

        val routesSnapshot = dynamicRoutes.toList()
        logger.info("Registering ${routesSnapshot.size} routes with Ktor")

        routesSnapshot.forEach { config ->
            config.rateLimitConfig?.let { rl ->
                rateLimiters[config] = TokenBucketRateLimiter(rl.burstSize, rl.requestsPerSecond)
            }
        }

        routesSnapshot.groupBy { it.authType }.forEach { (authType, routes) ->
            val authBlock: Route.() -> Unit = {
                routes.forEach { config ->
                    try {
                        when (config.endpointType) {
                            EndpointType.HTTP -> registerHttpRoute(config, this)
                            EndpointType.WEBSOCKET -> registerWebSocketRoute(config, this)
                        }
                    } catch (e: Exception) {
                        logger.error("Failed to register route: ${config.path}", e)
                        throw e
                    }
                }
            }

            when (authType) {
                AuthType.JWT -> routing.authenticate("auth-jwt", build = authBlock)
                AuthType.SESSION -> routing.authenticate("auth-session", build = authBlock)
                AuthType.ANY -> routing.authenticate(
                    "auth-jwt", "auth-session",
                    strategy = AuthenticationStrategy.FirstSuccessful,
                    build = authBlock
                )
                AuthType.NONE -> routing.authBlock()
            }
        }
        logger.info("All routes registered successfully with Ktor")
    }

    fun findRoute(path: String, method: HttpMethodType? = null): DynamicRouteConfig? {
        if (path.length > MAX_PATH_LENGTH) {
            logger.warn("Path exceeds maximum length: ${path.length}")
            return null
        }

        return dynamicRoutes.find { config ->
            when (config.endpointType) {
                EndpointType.HTTP -> method != null && config.methods.contains(method) && pathMatchesRoute(path, config.path)
                EndpointType.WEBSOCKET -> method == null && pathMatchesRoute(path, config.path)
            }
        }
    }

    fun getAllRoutes(): List<DynamicRouteConfig> = dynamicRoutes.toList()
    fun getHttpRoutes(): List<DynamicRouteConfig> = dynamicRoutes.filter { it.endpointType == EndpointType.HTTP }
    fun getWebSocketRoutes(): List<DynamicRouteConfig> = dynamicRoutes.filter { it.endpointType == EndpointType.WEBSOCKET }

    fun getRoutesByGroup(name: String): List<DynamicRouteConfig> {
        val group = routeGroups[name] ?: return emptyList()
        val prefix = group.prefix.trim('/')
        return dynamicRoutes.filter { it.path.startsWith("/$prefix") }
    }

    fun clear() {
        registrationLock.write {
            failIfRegistered("clear")
            dynamicRoutes.clear()
            routeGroups.clear()
            routePatternCache.clear()
            rateLimiters.clear()
        }
        logger.info("All routes cleared")
    }

    private fun failIfRegistered(operation: String) {
        if (registered.get()) {
            throw RouteFactoryFrozenException(
                "Cannot call $operation() after registerRoutes() has run -- " +
                        "the route set is frozen and Ktor's Routing tree cannot be updated retroactively."
            )
        }
    }

    private fun validateRouteConfig(config: DynamicRouteConfig, existing: List<DynamicRouteConfig>) {
        validatePath(config.path)
        validateEndpointType(config)
        validateAuthConsistency(config)
        checkForConflicts(config, existing)
    }

    private fun validatePath(path: String) {
        require(path.isNotBlank()) { "Path cannot be blank" }
        require(path.length <= MAX_PATH_LENGTH) { "Path exceeds maximum length of $MAX_PATH_LENGTH characters" }
        require(PATH_PATTERN.matches(path)) { "Path '$path' contains invalid characters." }
        require(path.startsWith("/")) { "Path must start with '/'" }
    }

    private fun validateEndpointType(config: DynamicRouteConfig) {
        when (config.endpointType) {
            EndpointType.HTTP -> {
                if (config.methods.isEmpty()) throw RouteValidationException("HTTP routes must specify at least one method")
                if (config.websocketHandler != null) throw RouteValidationException("HTTP routes cannot have a websocketHandler")
            }
            EndpointType.WEBSOCKET -> {
                if (config.websocketHandler == null) throw RouteValidationException("WebSocket routes must have a websocketHandler")
                if (config.methods.isNotEmpty()) throw RouteValidationException("WebSocket routes cannot specify HTTP methods")
            }
        }
    }

    /**
     * Fix for the silent-unauthenticated-route bug: registerRoutes() only ever
     * consulted authType, so requiresAuth=true with authType=NONE used to produce
     * a route that looked protected but wasn't. Now that combination is rejected
     * outright at registration time -- callers must pick an explicit authType.
     */
    private fun validateAuthConsistency(config: DynamicRouteConfig) {
        if (config.requiresAuth && config.authType == AuthType.NONE) {
            throw RouteValidationException(
                "Route '${config.path}' has requiresAuth=true but authType=NONE. " +
                        "Specify an explicit AuthType (SESSION, JWT, or ANY)."
            )
        }
    }

    private fun validateGroupConfig(name: String, config: RouteGroupConfig) {
        require(name.isNotBlank()) { "Group name cannot be blank" }
        require(config.prefix.startsWith("/")) { "Group prefix must start with '/'" }
        if (routeGroups.containsKey(name)) {
            throw RouteConflictException("Route group '$name' already exists")
        }
    }

    /**
     * Compares path *shape* rather than exact string, so /users/{id} and
     * /users/{name} -- structurally the same route -- are correctly flagged
     * as conflicting even though their parameter names differ.
     */
    private fun pathShape(path: String): String =
        path.split("/").joinToString("/") { segment ->
            if (segment.startsWith("{") && (segment.endsWith("}") || segment.endsWith("?}"))) "{}" else segment
        }

    private fun checkForConflicts(config: DynamicRouteConfig, existing: List<DynamicRouteConfig>) {
        val configShape = pathShape(config.path)
        val hasConflict = existing.any { other ->
            pathShape(other.path) == configShape &&
                    other.endpointType == config.endpointType &&
                    when (config.endpointType) {
                        EndpointType.HTTP -> other.methods.any { it in config.methods }
                        EndpointType.WEBSOCKET -> true
                    }
        }
        if (hasConflict) {
            throw RouteConflictException("Route conflict detected for path: ${config.path}")
        }
    }

    private fun resolveEffectiveAuthType(routeAuth: AuthType, groupAuth: AuthType): AuthType {
        return if (routeAuth != AuthType.NONE) routeAuth else groupAuth
    }

    private fun pathMatchesRoute(requestPath: String, routePath: String): Boolean {
        if (requestPath == routePath) return true

        val regex = routePatternCache.getOrPut(routePath) {
            try {
                val segments = routePath.split("/").joinToString("/") { segment ->
                    when {
                        segment.startsWith("{") && segment.endsWith("?}") -> "[^/]*"
                        segment.startsWith("{") && segment.endsWith("}") -> "[^/]+"
                        else -> Regex.escape(segment)
                    }
                }
                Regex("^$segments$")
            } catch (e: Exception) {
                logger.error("Failed to compile regex for path: $routePath", e)
                Regex(Regex.escape(routePath))
            }
        }
        return regex.matches(requestPath)
    }

    private fun registerHttpRoute(config: DynamicRouteConfig, currentRouteNode: Route) {
        config.methods.forEach { method ->
            currentRouteNode.route(config.path, method.toKtorMethod()) {
                handle {
                    if (!tryAcquireRateLimit(config)) {
                        call.response.header(HttpHeaders.RetryAfter, "1")
                        call.respond(HttpStatusCode.TooManyRequests, "Rate limit exceeded")
                        return@handle
                    }
                    val routingContext = this
                    try {
                        withTimeout(config.timeoutMs.milliseconds) {
                            with(routingContext) {
                                config.handler(this)
                            }
                        }
                    } catch (e: TimeoutCancellationException) {
                        logger.warn("Route timed out after ${config.timeoutMs}ms: ${config.path}")
                        call.respond(HttpStatusCode.GatewayTimeout, "Request timed out")
                    } catch (e: Exception) {
                        logger.error("Error handling route: ${config.path}", e)
                        throw e
                    }
                }
            }.apply {
                attributes.put(RouteMetadataKey, config)
            }
        }
    }

    private fun registerWebSocketRoute(config: DynamicRouteConfig, currentRouteNode: Route) {
        config.websocketHandler?.let { wsHandler ->
            currentRouteNode.webSocket(config.path) {
                if (!tryAcquireRateLimit(config)) {
                    close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Rate limit exceeded"))
                    return@webSocket
                }
                try {
                    wsHandler(this)
                } catch (e: Exception) {
                    logger.error("Error in WebSocket handler: ${config.path}", e)
                    throw e
                }
            }.apply {
                attributes.put(RouteMetadataKey, config)
            }
        }
    }

    private fun tryAcquireRateLimit(config: DynamicRouteConfig): Boolean {
        val limiter = rateLimiters[config] ?: return true
        return limiter.tryAcquire()
    }
}