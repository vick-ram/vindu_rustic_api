package org.example.controllers.frontend.admin

import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import org.example.di.Component
import org.example.di.Inject
import org.example.services.CartItemService
import org.example.services.OrderItemService
import org.example.services.OrderStatusHistoryService
import org.example.services.PaymentTransactionService

@Component
class AdminOrderOperationsController @Inject constructor(
    private val cartItemService: CartItemService,
    private val orderItemService: OrderItemService,
    private val orderStatusHistoryService: OrderStatusHistoryService,
    private val paymentTransactionService: PaymentTransactionService
) {
    fun Route.adminOrderOperationRoutes() {
        route("/admin/operations") {
            get("cart-items") {
                val cartId = call.queryParameters["cartId"]
                    ?: return@get respondAdminError("Cart items", "cartId is required")
                respondAdminList("Cart items", cartItemService.getItems(cartId))
            }
            get("order-items") {
                val orderId = call.queryParameters["orderId"]
                    ?: return@get respondAdminError("Order items", "orderId is required")
                respondAdminList("Order items", orderItemService.getOrderItems(orderId))
            }
            get("order-status-history") {
                val orderId = call.queryParameters["orderId"]
                    ?: return@get respondAdminError("Order status history", "orderId is required")
                respondAdminList("Order status history", orderStatusHistoryService.getStatusHistory(orderId))
            }
            get("payment-transactions/failed") {
                respondAdminList("Failed payment transactions", paymentTransactionService.getFailedTransactions())
            }
        }
    }
}
