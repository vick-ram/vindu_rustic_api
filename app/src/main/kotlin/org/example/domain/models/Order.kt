package org.example.domain.models

import kotlinx.datetime.LocalDateTime
import org.example.domain.validations.Validations
import org.example.utils.now
import java.io.Serializable
import java.math.BigDecimal

data class Cart(
    val id: String,
    val userId: String,
    val items: List<CartItem> = emptyList(),
    val totalQuantity: Int,
    val totalPrice: BigDecimal,
    val discount: Discount? = null,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
)

data class CartItem(
    val id: String,
    val cartId: String,
    val productId: String,
    val quantity: Int,
    val unitPrice: BigDecimal,
    val addedAt: LocalDateTime = LocalDateTime.now()
) {
    val total: BigDecimal
        get() = quantity.toBigDecimal() * unitPrice
}

data class Order(
    val id: String,
    val userId: String,
    val orderNumber: String,
    val items: List<OrderItem>,
    val status: OrderStatus,
    val paymentStatus: PaymentStatus,
    val shippingAddress: Address,
    val totalAmount: BigDecimal,
    val discount: Discount? = null,
    val shippingFee: BigDecimal = BigDecimal.ZERO,
    val notes: String? = null,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
): Serializable

data class OrderItem(
    val id: String,
    val orderId: String,
    val productId: String,
    val quantity: Int,
    val unitPrice: BigDecimal,
    val totalPrice: BigDecimal
)

enum class OrderStatus {
    PENDING, PROCESSING, SHIPPED, DELIVERED, CANCELLED, RETURNED
}

enum class PaymentStatus {
    UNPAID, PAID, REFUNDED, FAILED, PARTIALLY_PAID
}

data class Payment(
    val id: String,
    val orderId: String,
    val userId: String,
    val amount: BigDecimal,
    val status: PaymentStatus,
    val method: PaymentMethod,
    val transactionReference: String?,
    val paidAt: LocalDateTime?,
    val createdAt: LocalDateTime = LocalDateTime.now()
)

enum class PaymentMethod {
    CARD, MPESA, PAYPAL, CASH_ON_DELIVERY
}

data class Address(
    val id: String,
    val fullName: String,
    val phone: String,
    val email: String?,
    val street: String,
    val county: String,
    val postalCode: String
)


data class UpdateOrderStatusRequest(
    val orderId: String,
    val status: OrderStatus
) {
    fun validate(): UpdateOrderStatusRequest {
        Validations.validateAll(
            { Validations.validateNonEmpty(orderId, "Order Id") },
            { Validations.validateEnum<OrderStatus>(status, "Order Status") },
        )
        return this
    }
}

data class CartRequest(
    val productId: String,
    val quantity: Int = 1
) {
    fun validate(): CartRequest {
        Validations.validateAll(
            { Validations.validateNonEmpty(productId, "Product Id") },
            { Validations.validateNonEmpty(quantity.toString(), "Quantity") },
            { Validations.validateGreaterThan(quantity, 1, "Quantity") },
            { Validations.validateLessThan(quantity, 1, "Quantity") }
        )
        return this
    }
}

data class RemoveFromCart(
    val productId: String,
) {
    fun validate(): RemoveFromCart {
        Validations.validateNonEmpty(productId, "Product Id")
        return this
    }
}

data class UpdateCartQuantity(
    val productId: String,
    val quantity: Int = 1
)

