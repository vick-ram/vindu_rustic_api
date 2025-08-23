package org.example.routes

import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.thymeleaf.ThymeleafContent

fun Route.frontendRoutes() {
    route("/admin") {
        get("/login") {
            call.respond(ThymeleafContent("admin/pages/signin", mapOf()))
        }
//        Admin dashboard
        get {
            call.respond(ThymeleafContent("admin/pages/dashboard", mapOf()))
        }
    }
    route("/") {
        get("signin") {
            call.respond(ThymeleafContent("customer/pages/signin", mapOf()))
        }
        get("signup") {
            call.respond(ThymeleafContent("customer/pages/signup", mapOf()))
        }
        get {
            call.respond(ThymeleafContent("customer/pages/home", mapOf()))
        }
    }
}