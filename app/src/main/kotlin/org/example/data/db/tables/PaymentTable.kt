package org.example.data.db.tables

import org.example.domain.models.PaymentMethod
import org.example.domain.models.PaymentStatus
import org.example.utils.CustomTable
import org.example.utils.PGEnum
import org.example.utils.tsVector
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.datetime.datetime

object PaymentTable : CustomTable("payments") {
    val order = reference("order", OrderTable, ReferenceOption.CASCADE).index()
    val user = reference("user", UserTable, ReferenceOption.CASCADE).index()
    val amount = decimal("amount", 10, 2)
    val status = customEnumeration(
        name = "status",
        sql = "PaymentStatus",
        fromDb = { value -> PaymentStatus.valueOf(value as String) },
        toDb = { PGEnum("PaymentStatus", it) }).index()
    val method = customEnumeration(
        name = "method",
        sql = "PaymentMethod",
        fromDb = { value -> PaymentMethod.valueOf(value as String) },
        toDb = { PGEnum("PaymentMethod", it) }).index()
    val transactionReference = varchar("txn_ref", 100).nullable()
    val paidAt = datetime("paid_at").nullable()
    val tsv = tsVector("tsv")
}