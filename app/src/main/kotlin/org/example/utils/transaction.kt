package org.example.utils

import kotlinx.coroutines.Dispatchers
import org.example.data.db.config.DatabaseFactory
import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

suspend fun <T> suspendTransaction(block: suspend JdbcTransaction.() -> T): T {
    return newSuspendedTransaction(context = Dispatchers.IO, db = DatabaseFactory.db, statement = block)
}

//fun <T> suspendTransaction(block: Transaction.() -> T) : T {
//    return transaction(db = DatabaseFactory.db, statement = block)
//}