package org.example.controllers.frontend.admin

import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import org.example.di.Component
import org.example.di.Inject
import org.example.services.ProductVariantService

@Component
class AdminProductVariantController @Inject constructor(
    private val productVariantService: ProductVariantService
) {
    fun Route.adminProductVariantRoutes() {
        route("/admin/product-variants") {
            get {
                val productId = call.queryParameters["productId"]
                    ?: return@get respondAdminError("Product variants", "productId is required")
                respondAdminList("Product variants", productVariantService.getVariantsByProduct(productId), "products")
            }
        }
    }
}
