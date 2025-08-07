package org.example.data.db.tables

import org.example.domain.models.OrderStatus
import org.example.domain.models.PaymentMethod
import org.example.domain.models.PaymentStatus
import org.example.utils.CustomTable
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.datetime.datetime
import java.math.BigDecimal

object OrderTable : CustomTable("orders") {
    val user = reference("user", UserTable, ReferenceOption.CASCADE)
    val orderNumber = varchar("order_number", 50).uniqueIndex()
    val status = enumerationByName("status", 20, OrderStatus::class)
    val paymentStatus = enumerationByName("payment_status", 20, PaymentStatus::class)
    val shippingAddress = reference("shipping_address", AddressTable, ReferenceOption.CASCADE)
    val totalAmount = decimal("total_amount", 10, 2).default(BigDecimal.ZERO)
    val discount = reference("discount", DiscountTable, ReferenceOption.CASCADE).nullable()
    val shippingFee = decimal("shipping_fee", 10, 2).default(BigDecimal.ZERO)
    val notes = varchar("notes", 500).nullable()
}

object OrderItemTable : CustomTable("order_items") {
    val order = reference("order", OrderTable, ReferenceOption.CASCADE)
    val product = reference("product", ProductTable, ReferenceOption.CASCADE)
    val quantity = integer("quantity").default(1)
    val unitPrice = decimal("unit_price", 10, 2)
    val totalPrice = decimal("total_price", 10, 2).default(BigDecimal.ZERO)

    override val primaryKey = PrimaryKey(order, product) // Composite key
}

object AddressTable : CustomTable("addresses") {
    val user = reference("user", UserTable, ReferenceOption.CASCADE)
    val fullName = varchar("full_name", 100)
    val phone = varchar("phone", 20)
    val email = varchar("email", 100).nullable()
    val street = varchar("street", 255)
    val county = varchar("county", 100)
    val region = varchar("region", 100)
    val postalCode = varchar("postal_code", 20)
}

object PaymentTable : CustomTable("payments") {
    val order = reference("order", OrderTable, ReferenceOption.CASCADE)
    val user = reference("user", UserTable, ReferenceOption.CASCADE)
    val amount = decimal("amount", 10, 2)
    val status = enumerationByName("status", 30, PaymentStatus::class)
    val method = enumerationByName("method", 30, PaymentMethod::class)
    val transactionReference = varchar("txn_ref", 100).nullable()
    val paidAt = datetime("paid_at").nullable()
}

