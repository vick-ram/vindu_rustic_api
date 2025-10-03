package org.example.plugins

import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.routing.*
import org.example.routes.backendRoutes
import org.example.routes.frontendRoutes
import org.example.routes.serveStaticContent
import org.example.utils.SecurityConfig

fun Application.configureRouting(config: SecurityConfig) {
    /* Ignore Trailing slashes in routes */
    install(IgnoreTrailingSlash)

    /*Backend routing*/
    routing {
        route("/api/v1") {
            backendRoutes(config.issuer, config.audience, config.secret)
        }

        /*Frontend routing*/
        frontendRoutes()

        /*Static files*/
        serveStaticContent()
    }
}

