package org.example.routes

import io.ktor.server.routing.*
import org.example.controllers.frontend.ecommerce.AuthController
import org.example.controllers.frontend.EcommerceController
import org.example.controllers.frontend.RootController
import org.example.controllers.frontend.admin.AdminCustomerOperationsController
import org.example.controllers.frontend.admin.AdminFulfillmentController
import org.example.controllers.frontend.admin.AdminInventoryController
import org.example.controllers.frontend.admin.AdminOrderOperationsController
import org.example.controllers.frontend.admin.AdminProductVariantController
import org.koin.ktor.ext.inject

fun Route.frontendRoutes() {
    val rootController by inject<RootController>()
    val authController by inject<AuthController>()
    val ecommerceController by inject<EcommerceController>()
    val adminInventoryController by inject<AdminInventoryController>()
    val adminFulfillmentController by inject<AdminFulfillmentController>()
    val adminOrderOperationsController by inject<AdminOrderOperationsController>()
    val adminCustomerOperationsController by inject<AdminCustomerOperationsController>()
    val adminProductVariantController by inject<AdminProductVariantController>()

    with(rootController) { routes() }

    with(authController) { authRoutes() }

    with(ecommerceController) { ecommerceRoutes() }

    with(adminInventoryController) { adminInventoryRoutes() }

    with(adminFulfillmentController) { adminFulfillmentRoutes() }

    with(adminOrderOperationsController) { adminOrderOperationRoutes() }

    with(adminCustomerOperationsController) { adminCustomerOperationRoutes() }

    with(adminProductVariantController) { adminProductVariantRoutes() }
}
