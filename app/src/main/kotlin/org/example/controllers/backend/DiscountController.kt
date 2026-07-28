package org.example.controllers.backend

import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route

class DiscountController(private val discountService: DiscountService) {
    fun Route.discountRoutes() {
        route("/discount/") {
            post {

            }
        }
    }
}