package org.example.controllers

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.util.toMap
import org.example.domain.models.Product
import org.example.services.ProductService
import org.example.utils.respondApi

class ProductController(private val productService: ProductService) {
    fun Route.productRoutes() {
        route("/products/") {
            post {
                val productRequest = call.receive<Product>().validate()
                val newProduct = productService.createProduct(productRequest)
                call.respondApi(
                    status = HttpStatusCode.Created,
                    data = newProduct,
                    message = "Product created successfully"
                )
            }

            put("{id}") {
                val productId = call.parameters["id"] ?: ""
                val updateProductRequest = call.receive<Product>()
                val updatedProduct = productService.updateProduct(productId, updateProductRequest)
                call.respondApi(
                    status = HttpStatusCode.Accepted,
                    data = updatedProduct,
                    message = "Product updated successfully"
                )
            }

            get("{id}") {
                val productId = call.parameters["id"] ?: ""
                val product = productService.getProduct(productId)
                call.respondApi(
                    data = product,
                    message = "Product fetched successfully"
                )
            }

            get {
                val offset = call.request.queryParameters["offset"]?.toLongOrNull() ?: 0
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 10
                val queryParams = call.request.queryParameters.toMap().mapValues { it.value.firstOrNull() ?: "" }
                    .filterKeys { it != "offset" && it != "limit" }

                val products = productService.getProducts(offset, limit, queryParams)
                call.respondApi(
                    data = products,
                    message = "Products fetched successfully"
                )
            }

            delete("{id}") {
                val productId = call.parameters["id"] ?: ""
                val deleted = productService.deleteProduct(productId)
                if (deleted) {
                    call.respondApi<Unit>(
                        status = HttpStatusCode.NoContent,
                        message = "Product deleted successfully"
                    )
                }
            }
            route("images/") {
                post {

                }
            }
        }
    }
}