package org.example.domain.models.sales

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.example.data.db.config.Ulid
import org.example.domain.validations.Email
import org.example.utils.BigDecimalSerializer
import org.example.utils.InetAddressSerializer
import org.example.utils.OffsetDateTimeSerializer
import java.math.BigDecimal
import java.net.InetAddress
import java.time.OffsetDateTime

@Serializable
data class Order(
    val id: String = Ulid.generate(),

    @SerialName(value = "order_number")
    val orderNumber: String,

    @SerialName(value = "user_id")
    val userId: String? = null,

    @field:Email
    val email: String,

    @SerialName("shopping_address_id")
    val shippingAddressId: String,

    @SerialName("billing_address_id")
    val billingAddressId: String? = null,

    val status: String = "pending",

    @SerialName(value = "payment_status")
    val paymentStatus: String = "pending",

    @SerialName(value = "fulfillment_status")
    val fulfillmentStatus: String = "unfulfilled",

    val currency: String = "kes",

    @Serializable(with = BigDecimalSerializer::class)
    val subtotal: BigDecimal = BigDecimal.ZERO,

    @Serializable(with = BigDecimalSerializer::class)
    @SerialName(value = "shipping_cost")
    val shippingCost: BigDecimal,

    @Serializable(with = BigDecimalSerializer::class)
    val taxAmount: BigDecimal,

    @Serializable(with = BigDecimalSerializer::class)
    @SerialName(value = "discount_amount")
    val discountAmount: BigDecimal = BigDecimal.ZERO,

    @Serializable(with = BigDecimalSerializer::class)
    @SerialName(value = "total_amount")
    val totalAmount: BigDecimal,

    @SerialName("coupon_code")
    val couponCode: String? = null,

    val notes: String? = null,

    @Serializable(with = InetAddressSerializer::class)
    @SerialName("ip_address")
    val ipAddress: InetAddress? = null,

    @SerialName("user_agent")
    val userAgent: String? = null,

    @Serializable(with = OffsetDateTimeSerializer::class)
    @SerialName(value = "placed_at")
    val placedAt: OffsetDateTime = OffsetDateTime.now(),

    @Serializable(with = OffsetDateTimeSerializer::class)
    @SerialName(value = "updated_at")
    val updatedAt: OffsetDateTime = OffsetDateTime.now()
) {
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
                "date" to order.placedAt
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

