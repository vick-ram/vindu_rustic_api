package org.example.controllers.frontend.admin

import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import org.example.di.Component
import org.example.di.Inject
import org.example.services.InventoryMovementService
import org.example.services.InventoryReservationService
import org.example.services.InventoryService

@Component
class AdminInventoryController @Inject constructor(
    private val inventoryService: InventoryService,
    private val movementService: InventoryMovementService,
    private val reservationService: InventoryReservationService
) {
    fun Route.adminInventoryRoutes() {
        route("/admin/inventory") {
            get {
                respondAdminList("Inventory", inventoryService.getInventories(), "inventory")
            }
            get("low-stock") {
                val threshold = call.queryParameters["threshold"]?.toIntOrNull() ?: 10
                respondAdminList("Low-stock inventory", inventoryService.getLowStockProducts(threshold), "inventory")
            }
            get("movements") {
                respondAdminList("Inventory movements", movementService.getMovements(), "inventory")
            }
            get("movements/variant") {
                val variantId = call.queryParameters["variantId"]
                    ?: return@get respondAdminError("Inventory movements", "variantId is required")
                respondAdminList("Inventory movements", movementService.getMovementsByVariant(variantId), "inventory")
            }
            get("reservations/variant") {
                val variantId = call.queryParameters["variantId"]
                    ?: return@get respondAdminError("Inventory reservations", "variantId is required")
                respondAdminList("Inventory reservations", reservationService.getReservationsByVariant(variantId), "inventory")
            }
            get("reservations/cart") {
                val cartId = call.queryParameters["cartId"]
                    ?: return@get respondAdminError("Inventory reservations", "cartId is required")
                respondAdminList("Cart reservations", reservationService.getReservationsByCart(cartId), "inventory")
            }
            get("reservations/order") {
                val orderId = call.queryParameters["orderId"]
                    ?: return@get respondAdminError("Inventory reservations", "orderId is required")
                respondAdminList("Order reservations", reservationService.getReservationsByOrder(orderId), "inventory")
            }
        }
    }
}
