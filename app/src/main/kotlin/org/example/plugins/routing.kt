package org.example.plugins

import io.ktor.server.application.Application
import io.ktor.server.http.content.staticResources
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import org.example.routes.frontend
import org.example.routes.userRoutes

fun Application.configureRouting(issuer: String, audience: String, secret: String) {
    /*Backend routing*/
    routing {
        route("/api/v1") {
            userRoutes(issuer, audience, secret)
        }
    }

    /*Frontend routing*/
    routing {
        frontend()
    }

    /*Static files*/
    routing {
        staticResources("/resources", "static")
        staticResources("META-INF/resources/webjars", "webjars")
    }

    /*Swagger documentation*/
    routing {
        swaggerUI(path = "docs", swaggerFile = "openapi/documentation.yaml")
    }

}