package org.example.routes

import io.ktor.server.http.content.staticFiles
import io.ktor.server.http.content.staticResources
import io.ktor.server.routing.Route
import java.io.File

fun Route.serveStaticContent() {
    staticFiles("/media/products", File("uploads/products"))
    staticFiles("/media/categories", File("uploads/categories"))

    staticResources("/resources", "static")
    staticResources("META-INF/resources/webjars", "webjars")
}