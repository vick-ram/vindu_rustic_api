package org.example.plugins

import io.ktor.server.application.*
import io.ktor.server.application.install
import io.ktor.server.response.respondText
import io.ktor.server.routing.*
import io.ktor.server.webjars.Webjars
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.pingPeriod
import io.ktor.server.websocket.timeout
import org.example.config.AppConfig
import org.example.config.ApplicationPlugin
import org.example.config.DynamicRouteFactory
import org.example.routes.backendRoutes
import org.example.routes.frontendRoutes
import org.example.routes.serveStaticContent
import kotlin.time.Duration.Companion.seconds

class RoutingModule(private val factory: DynamicRouteFactory) : ApplicationPlugin {
    override fun install(application: Application) {
        val config = AppConfig.load(application)

        application.install(IgnoreTrailingSlash)

        application.install(Webjars) {
            path = "/webjars"
        }

        application.install(WebSockets) {
            pingPeriod = 15.seconds
            timeout = 30.seconds
            maxFrameSize = Long.MAX_VALUE
            masking = false
        }

        application.routing {
            get("/") {
                call.respondText("Hello from vindu rustic server")
            }
            backendRoutes(factory, config.security.issuer, config.security.audience, config.security.secret)
            /*Frontend routing*/
            frontendRoutes()
            /*Static files*/
            serveStaticContent()
            // Register all routes
            factory.registerRoutes(this)
        }
    }
}
