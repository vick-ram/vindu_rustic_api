package org.example.data.db.tables

import org.example.utils.CustomTable
import org.jetbrains.exposed.v1.core.ReferenceOption
import java.math.BigDecimal

object CartTable: CustomTable("carts") {
    val user = reference("user", UserTable, ReferenceOption.CASCADE)
    val totalQuantity = integer("total_quantity").default(0)
    val totalPrice = decimal("total_price", 10, 2).default(BigDecimal.ZERO)
    val discount = reference("discount", DiscountTable, ReferenceOption.CASCADE).nullable()
}

object CartItemTable: CustomTable("cart_items") {
    val cart = reference("cart", CartTable, ReferenceOption.CASCADE)
    val product = reference("product", ProductTable, ReferenceOption.CASCADE)
    val quantity = integer("quantity")
    val unitPrice = decimal("unit_price", 10, 2)
    val totalPrice = decimal("total_price", 10, 2)

    override val primaryKey: PrimaryKey?
        get() = PrimaryKey(cart, product)
}