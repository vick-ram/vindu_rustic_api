package org.example.controllers.backend

import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import io.ktor.util.*
import org.example.domain.models.CreateProductRequest
import org.example.domain.models.Dimension
import org.example.domain.models.Product
import org.example.plugins.BadRequestException
import org.example.services.ProductService
import org.example.utils.Json
import org.example.utils.respondApi

class ProductController(private val productService: ProductService) {
    fun Route.productRoutes() {
        route("/products/") {
            post {
                val multipart = call.receiveMultipart()
                val mediaFiles: MutableList<PartData.FileItem> = mutableListOf()
                val request = CreateProductRequest.formMultipart(multipart, mediaFiles)
                try {
                    val product = productService.createProduct(request, mediaFiles)
                    call.respondApi(
                        status = HttpStatusCode.Created,
                        data = product,
                        message = "Product created successfully"
                    )
                } catch (e: Exception) {
                    throw e
                }
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