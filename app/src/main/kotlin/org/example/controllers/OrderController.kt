package org.example.controllers

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.example.domain.models.UpdateOrderStatusRequest
import org.example.services.OrderService
import org.example.utils.respondApi

class OrderController(private val orderService: OrderService) {
    fun Route.orderRoutes() {
        route("/orders/") {
            authenticate("auth-jwt") {
                post {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.subject ?: ""

                    val newOrder = orderService.createOrder(userId)
                    call.respondApi(
                        status = HttpStatusCode.Created,
                        data = newOrder,
                        message = "Order created successfully"
                    )
                }
            }

            get("{id}") {
                val orderId = call.parameters["id"] ?: ""
                val orderData = orderService.getOrderById(orderId)
                call.respondApi(
                    data = orderData,
                    message = "Order fetched successfully"
                )
            }

            get("{userId}") {
                val userId = call.parameters["userId"] ?: ""
                val offset = call.request.queryParameters["offset"]?.toIntOrNull() ?: 0
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 10
                val orders = orderService.getOrderByUser(userId, offset, limit)

                call.respondApi(
                    data = orders,
                    message = "Orders for user fetched successfully"
                )
            }

            patch("status") {
                val orderRequest = call.receive<UpdateOrderStatusRequest>().validate()
                val updatedOrder = orderService.updateOrderStatus(orderRequest.orderId, orderRequest.status)
                call.respondApi(
                    status = HttpStatusCode.Accepted,
                    data = updatedOrder,
                    message = "Order status updated successfully"
                )
            }

            delete("{id}") {
                val orderId = call.parameters["id"] ?: ""
                val deleted = orderService.deleteOrder(orderId)

                if (deleted) {
                    call.respondApi<Unit>(
                        status = HttpStatusCode.NoContent,
                        message = "Order deleted successfully"
                    )
                }
            }

        }
    }
}