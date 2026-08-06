package org.example.controllers.frontend.admin

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receiveParameters
import io.ktor.server.routing.RoutingContext
import org.example.config.AuthType
import org.example.config.DynamicRouteConfig
import org.example.config.DynamicRouteFactory
import org.example.config.HttpMethodType
import org.example.config.RouteGroupConfig
import org.example.di.Component
import org.example.di.Inject
import org.example.domain.models.catalog.Product
import org.example.domain.validations.validate
import org.example.services.ProductService
import org.example.utils.respondHtml

@Component
class AdminProductController @Inject constructor(
    private val productService: ProductService
) {
    fun registerProductRoutes(factory: DynamicRouteFactory) {
        val createHandler: suspend RoutingContext.() -> Unit = {
            val formParams = call.receiveParameters()
            val validProduct = Product.formParameters(formParams).validate()
            val created = productService.createProduct(validProduct)
            call.respondHtml(
                template = "admin/products/create",
                data = created,
                message = "Product created successfully",
            )
        }

        val updateHandler: suspend RoutingContext.() -> Unit = updateHandler@{
            val id = call.queryParameters["id"].orEmpty()
            val formParams = call.receiveParameters()

            if (id.isEmpty()) {
                call.respondHtml<Unit>(
                    template = "admin/products/update",
                    status = HttpStatusCode.BadRequest,
                    errors = listOf("Product ID is required")
                )
                return@updateHandler
            }

            val validProduct = Product.formParameters(formParams).validate()
            val updated = productService.updateProduct(id, validProduct)
            if (updated != null) {
                call.respondHtml(
                    template = "admin/products/update",
                    data = updated,
                    message = "Product updated successfully"
                )
            } else {
                call.respondHtml<Unit>(
                    template = "admin/products/update",
                    status = HttpStatusCode.NotFound,
                    errors = listOf("Product ID is required")
                )
            }
        }

        val searchHandler: suspend RoutingContext.() -> Unit = {
            val query = call.queryParameters["q"] ?: call.queryParameters["query"].orEmpty()
            val status = call.queryParameters["status"]
            val productType = call.queryParameters["productType"]
            val brand = call.queryParameters["brand"]
            val offset = call.queryParameters["offset"]?.toIntOrNull() ?: 0
            val limit = call.queryParameters["limit"]?.toIntOrNull() ?: 20

            val results = productService.getProductsForAdmin(
                status = status,
                productType = productType,
                brand = brand,
                search = query.ifEmpty { null },
                offset = offset,
                limit = limit
            )

            call.respondHtml(
                template = "admin/products/list",
                data = results,
                message = "Found ${results.total} products"
            )
        }

        val statsHandler: suspend RoutingContext.() -> Unit = {
            val stats = productService.getProductStats()
            call.respondHtml(
                template = "admin/products/stats",
                data = stats
            )
        }

        val latestHandler: suspend RoutingContext.() -> Unit = {
            val limit = call.queryParameters["limit"]?.toIntOrNull() ?: 10
            val status = call.queryParameters["status"] ?: "published"

            val products = productService.getLatestProducts(limit, status)
            call.respondHtml(
                template = "admin/products/latest",
                data = products,
                message = "Showing latest $limit products"
            )
        }

        val lowStockHandler: suspend RoutingContext.() -> Unit = {
            val warehouseId = call.queryParameters["warehouseId"]
            val threshold = call.queryParameters["threshold"]?.toIntOrNull() ?: 10
            val limit = call.queryParameters["limit"]?.toIntOrNull() ?: 20

            val products = productService.getLowStockProducts(warehouseId, threshold, limit)
            call.respondHtml(
                template = "admin/products/low-stock",
                data = products,
                message = "Found ${products.size} low stock products (threshold: $threshold)"
            )
        }

        val deleteHandler: suspend RoutingContext.() -> Unit = deleteHandler@{
            val id = call.queryParameters["id"].orEmpty()

            if (id.isEmpty()) {
                call.respondHtml<Unit>(
                    template = "admin/products/list",
                    status = HttpStatusCode.BadRequest,
                    errors = listOf("Product ID is required")
                )
                return@deleteHandler
            }

            val deleted = productService.deleteProduct(id)
            if (deleted) {
                call.respondHtml<Unit>(
                    template = "admin/products/list",
                    message = "Product deleted successfully"
                )
            } else {
                call.respondHtml<Unit>(
                    template = "admin/products/list",
                    status = HttpStatusCode.NotFound,
                    errors = listOf("Product not found or could not be deleted")
                )
            }
        }

        val statusUpdateHandler: suspend RoutingContext.() -> Unit = {
            val id = call.queryParameters["id"].orEmpty()
            val status = call.queryParameters["status"].orEmpty()
            val ids = call.queryParameters.getAll("ids")
            if (id.isNotEmpty()) {
                // Single product status update
                val updated = productService.updateProductStatus(id, status)
                if (updated != null) {
                    call.respondHtml(
                        template = "admin/products/list",
                        data = updated,
                        message = "Product status updated to $status"
                    )
                } else {
                    call.respondHtml<Unit>(
                        template = "admin/products/list",
                        status = HttpStatusCode.NotFound,
                        errors = listOf("Product not found")
                    )
                }
            } else if (!ids.isNullOrEmpty()) {
                // Bulk status update
                val count = productService.bulkUpdateProductStatus(ids, status)
                call.respondHtml<Unit>(
                    template = "admin/products/list",
                    message = "Updated $count products to status: $status"
                )
            } else {
                call.respondHtml<Unit>(
                    template = "admin/products/list",
                    status = HttpStatusCode.BadRequest,
                    errors = listOf("Product ID or IDs required")
                )
            }

        }


        val group = RouteGroupConfig(
            prefix = "/products"
        )

        val routes = listOf(
            DynamicRouteConfig(
                path = "/new",
                methods = setOf(HttpMethodType.POST),
                handler = createHandler,
                name = "CreateProduct",
                requiresAuth = true,
                authType = AuthType.SESSION
            ),
            DynamicRouteConfig(
                path = "/update",
                methods = setOf(HttpMethodType.PUT, HttpMethodType.POST),
                handler = updateHandler,
                name = "UpdateProduct",
                requiresAuth = true,
                authType = AuthType.SESSION
            ),
            DynamicRouteConfig(
                path = "/search",
                methods = setOf(HttpMethodType.GET),
                handler = searchHandler,
                name = "SearchProducts",
                requiresAuth = true,
                authType = AuthType.SESSION
            ),
            DynamicRouteConfig(
                path = "/stats",
                methods = setOf(HttpMethodType.GET),
                handler = statsHandler,
                name = "ProductStats",
                requiresAuth = true,
                authType = AuthType.SESSION
            ),
            DynamicRouteConfig(
                path = "/latest",
                methods = setOf(HttpMethodType.GET),
                handler = latestHandler,
                name = "LatestProducts",
                requiresAuth = true,
                authType = AuthType.SESSION
            ),
            DynamicRouteConfig(
                path = "/low-stock",
                methods = setOf(HttpMethodType.GET),
                handler = lowStockHandler,
                name = "LowStockProducts",
                requiresAuth = true,
                authType = AuthType.SESSION
            ),
            DynamicRouteConfig(
                path = "/delete",
                methods = setOf(HttpMethodType.DELETE, HttpMethodType.POST),
                handler = deleteHandler,
                name = "DeleteProduct",
                requiresAuth = true,
                authType = AuthType.SESSION
            ),
            DynamicRouteConfig(
                path = "/status",
                methods = setOf(HttpMethodType.PUT, HttpMethodType.POST),
                handler = statusUpdateHandler,
                name = "UpdateProductStatus",
                requiresAuth = true,
                authType = AuthType.SESSION
            )
        )
        factory.registerGroup("ProductRoutesGroup", group, routes)
    }
}