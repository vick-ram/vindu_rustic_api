package org.example.controllers.backend

import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import io.ktor.util.*
import org.example.config.AuthType
import org.example.config.DynamicRouteConfig
import org.example.config.DynamicRouteFactory
import org.example.config.HttpMethodType
import org.example.config.RouteGroupConfig
import org.example.domain.models.catalog.CreateProductRequest
import org.example.domain.models.catalog.Product
import org.example.services.ProductService
import org.example.utils.respondApi

class ProductController(private val productService: ProductService) {
    fun registerProductRoutes(factory: DynamicRouteFactory) {
        val createProductHandler: RoutingHandler = {
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

        val updateProductHandler: RoutingHandler = {
            val productId = call.parameters["id"] ?: ""
            val updateProductRequest = call.receive<Product>()
            val updatedProduct = productService.updateProduct(productId, updateProductRequest)
            call.respondApi(
                status = HttpStatusCode.Accepted,
                data = updatedProduct,
                message = "Product updated successfully"
            )
        }

        val getProductHandler: RoutingHandler = {
            val productId = call.parameters["id"] ?: ""
            val product = productService.getProduct(productId)
            call.respondApi(
                data = product,
                message = "Product fetched successfully"
            )
        }

        val getProductsHandler: RoutingHandler = {
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

        val deleteProductHandler: RoutingHandler = {
            val productId = call.parameters["id"] ?: ""
            val deleted = productService.deleteProduct(productId)
            if (deleted) {
                call.respondApi<Unit>(
                    status = HttpStatusCode.NoContent,
                    message = "Product deleted successfully"
                )
            }
        }

        val group = RouteGroupConfig(
            prefix = "/api/products",
            version = "1.0.0"
        )

        val routes = listOf(
            DynamicRouteConfig(
                path = "",
                methods = setOf(HttpMethodType.POST),
                handler = createProductHandler,
                name = "CreateProduct",
                requiresAuth = true,
                authType = AuthType.JWT
            ),
            DynamicRouteConfig(
                path = "/{id}",
                methods = setOf(HttpMethodType.PUT),
                handler = updateProductHandler,
                name = "UpdateProduct",
                requiresAuth = true,
                authType = AuthType.JWT
            ),
            DynamicRouteConfig(
                path = "/{id}",
                methods = setOf(HttpMethodType.GET),
                handler = getProductHandler,
                name = "GetProduct",
                requiresAuth = true,
                authType = AuthType.JWT
            ),
            DynamicRouteConfig(
                path = "",
                methods = setOf(HttpMethodType.GET),
                handler = getProductsHandler,
                name = "GetListOfProducts",
                requiresAuth = true,
                authType = AuthType.JWT
            ),
            DynamicRouteConfig(
                path = "/{id}",
                methods = setOf(HttpMethodType.DELETE),
                handler = deleteProductHandler,
                name = "DeleteProduct",
                requiresAuth = true,
                authType = AuthType.JWT
            ),
        )

        factory.registerGroup("ProductRouteGroup", group, routes)
    }
}