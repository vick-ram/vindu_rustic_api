package org.example.controllers

import io.ktor.server.routing.Route
import io.ktor.server.routing.route
import org.example.services.ProductService

class ProductController(private val productService: ProductService) {
    fun Route.productRoutes() {
        route("/products/") {

        }
    }
}