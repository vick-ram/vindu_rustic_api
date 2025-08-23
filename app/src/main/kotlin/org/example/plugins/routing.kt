package org.example.plugins

import io.ktor.server.application.Application
import io.ktor.server.http.content.staticResources
import io.ktor.server.plugins.openapi.openAPI
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import org.example.routes.backendRoutes
import org.example.routes.frontendRoutes

fun Application.configureRouting(issuer: String, audience: String, secret: String) {
    /*Backend routing*/
    routing {
        route("/api/v1") {
            backendRoutes(issuer, audience, secret)
        }

        /*Frontend routing*/
        frontendRoutes()

        /*Static files*/
        staticResources("/resources", "static")
        staticResources("META-INF/resources/webjars", "webjars")

        /*Swagger documentation*/
        swaggerUI(path = "docs", swaggerFile = "openapi/documentation.yaml") {
            version = "4.15.5"
        }
        openAPI(path = "openapi", swaggerFile = "openapi/documentation.yaml")
    }
}

