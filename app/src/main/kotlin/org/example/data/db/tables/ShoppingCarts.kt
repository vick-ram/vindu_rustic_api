package org.example.data.db.tables

import org.example.data.db.config.CustomTable
import org.jetbrains.exposed.v1.core.or
import java.util.*
import kotlin.uuid.ExperimentalUuidApi

object ShoppingCarts : CustomTable("shopping_carts") {
    val userId = reference("user_id", Users).nullable()
    @OptIn(ExperimentalUuidApi::class)
    val guestToken = uuid("guest_token").default(UUID.randomUUID())

    init {
        check("cart_has_identity") { (userId.isNotNull()) or (guestToken.isNotNull()) }
    }
}