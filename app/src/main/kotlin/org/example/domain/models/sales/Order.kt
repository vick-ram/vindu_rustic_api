package org.example.domain.models.sales

import com.google.gson.annotations.SerializedName
import kotlinx.datetime.LocalDateTime
import org.example.domain.validations.Email
import org.example.utils.now
import org.example.utils.shortUUID
import org.example.utils.toCustomFormat
import java.io.Serializable
import java.math.BigDecimal
import java.net.InetAddress
import java.time.OffsetDateTime

data class Order(
    val id: String = shortUUID(),

    @SerializedName(value = "order_number")
    val orderNumber: String,

    @SerializedName(value = "user_id")
    val userId: String? = null,

    @field:Email
    val email: String,

    @SerializedName("shopping_address_id")
    val shippingAddressId: String,

    @SerializedName("billing_address_id")
    val billingAddressId: String? = null,

    val status: String = "pending",

    @SerializedName(value = "payment_status")
    val paymentStatus: String = "pending",

    @SerializedName(value = "fulfillment_status")
    val fulfillmentStatus: String = "unfulfilled",

    val currency: String = "kes",
    val subtotal: BigDecimal = BigDecimal.ZERO,

    @SerializedName(value = "shipping_cost")
    val shippingCost: BigDecimal,

    val taxAmount: BigDecimal,

    @SerializedName(value = "discount_amount")
    val discountAmount: BigDecimal = BigDecimal.ZERO,

    @SerializedName(value = "total_amount")
    val totalAmount: BigDecimal,

    @SerializedName("coupon_code")
    val couponCode: String? = null,

    val notes: String? = null,

    @SerializedName("ip_address")
    val ipAddress: InetAddress? = null,

    @SerializedName("user_agent")
    val userAgent: String? = null,

    @SerializedName(value = "placed_at")
    val placedAt: OffsetDateTime = OffsetDateTime.now(),

    @SerializedName(value = "updated_at")
    val updatedAt: OffsetDateTime = OffsetDateTime.now()
): Serializable {
    companion object {

        val columns: List<Map<String, Any>> = listOf(
            mapOf("key" to "id", "label" to "id"),
            mapOf("key" to "order", "label" to "orders", "sortable" to true),
            mapOf("key" to "status", "label" to "status", "sortable" to true),
            mapOf("key" to "payment_status", "label" to "payment status", "sortable" to true),
            mapOf("key" to "createdAt", "label" to "date", "sortable" to true),
        )

        fun toRows(orders: List<Order>): List<Map<String, Any?>> = orders.map { order ->
            mapOf(
                "id" to order.id,
                "order" to order.orderNumber,
                "status" to order.status,
                "payment_status" to order.paymentStatus,
                "date" to order.placedAt.toCustomFormat()
            )
        }
    }
}

enum class OrderStatus {
    PENDING, PROCESSING, SHIPPED, DELIVERED, CANCELLED, RETURNED
}

enum class PaymentStatus {
    UNPAID, PAID, REFUNDED, FAILED, PARTIALLY_PAID
}

enum class PaymentMethod {
    CARD, MPESA, PAYPAL, CASH_ON_DELIVERY
}


data class UpdateOrderStatusRequest(
    val orderId: String,
    val status: OrderStatus
)
//{
//    fun validate(): UpdateOrderStatusRequest {
//        Validations.validateAll(
//            { Validations.validateNonEmpty(orderId, "Order Id") },
//            { Validations.validateEnum<OrderStatus>(status, "Order Status") },
//        )
//        return this
//    }
//}

data class CartRequest(
    val productId: String,
    val quantity: Int = 1
)
//{
//    fun validate(): CartRequest {
//        Validations.validateAll(
//            { Validations.validateNonEmpty(productId, "Product Id") },
//            { Validations.validateNonEmpty(quantity.toString(), "Quantity") },
//            { Validations.validateGreaterThan(quantity, 1, "Quantity") },
//            { Validations.validateLessThan(quantity, 1, "Quantity") }
//        )
//        return this
//    }
//}

data class RemoveFromCart(
    val productId: String,
)
//{
//    fun validate(): RemoveFromCart {
//        Validations.validateNonEmpty(productId, "Product Id")
//        return this
//    }
//}

data class UpdateCartQuantity(
    val productId: String,
    val quantity: Int = 1
)

