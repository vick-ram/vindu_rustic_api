package org.example.domain.models

import kotlinx.datetime.LocalDateTime
import java.math.BigDecimal

data class Cart(
    val id: String,
    val user: User,
    val items: List<CartItem> = emptyList(),
    val totalQuantity: Int,
    val totalPrice: BigDecimal,
    val discount: Discount? = null,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

data class CartItem(
    val id: String,
    val cartId: String,
    val product: Product,
    val quantity: Int,
    val unitPrice: BigDecimal,
    val totalPrice: BigDecimal,
    val addedAt: LocalDateTime
)

data class Order(
    val id: String,
    val user: User,
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
)

data class OrderItem(
    val id: String,
    val order: Order,
    val product: Product,
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
    val order: Order,
    val user: User,
    val amount: BigDecimal,
    val status: PaymentStatus,
    val method: PaymentMethod,
    val transactionReference: String?,
    val paidAt: LocalDateTime?,
    val createdAt: LocalDateTime
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
