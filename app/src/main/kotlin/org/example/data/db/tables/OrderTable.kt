package org.example.data.db.tables

import org.example.domain.models.OrderStatus
import org.example.domain.models.PaymentMethod
import org.example.domain.models.PaymentStatus
import org.example.utils.CustomTable
import org.example.utils.PGEnum
import org.example.utils.tsVector
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.datetime.datetime
import java.math.BigDecimal

object OrderTable : CustomTable("orders") {
    val user = reference("user", UserTable, ReferenceOption.CASCADE)
    val orderNumber = varchar("order_number", 50).uniqueIndex()
    val status = customEnumeration(
        name = "status",
        sql = "OrderStatus",
        fromDb = { value -> OrderStatus.valueOf(value as String) },
        toDb = { PGEnum("OrderStatus", it) }).index()
    val paymentStatus = customEnumeration(
        name = "payment_status",
        sql = "PaymentStatus",
        fromDb = { value -> PaymentStatus.valueOf(value as String) },
        toDb = { PGEnum("PaymentStatus", it) }).index()
    val shippingAddress = reference("shipping_address", AddressTable, ReferenceOption.CASCADE)
    val totalAmount = decimal("total_amount", 10, 2).default(BigDecimal.ZERO)
    val discount = reference("discount", DiscountTable, ReferenceOption.CASCADE).nullable()
    val shippingFee = decimal("shipping_fee", 10, 2).default(BigDecimal.ZERO)
    val notes = varchar("notes", 500).nullable()
    val tsv = tsVector("tsv")
}

object OrderItemTable : CustomTable("order_items") {
    val order = reference("order", OrderTable, ReferenceOption.CASCADE)
    val product = reference("product", ProductTable, ReferenceOption.CASCADE)
    val quantity = integer("quantity").default(1)
    val unitPrice = decimal("unit_price", 10, 2)
    val totalPrice = decimal("total_price", 10, 2).default(BigDecimal.ZERO)

    override val primaryKey = PrimaryKey(order, product) // Composite key
}


