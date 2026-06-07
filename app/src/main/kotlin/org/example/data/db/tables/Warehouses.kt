package org.example.data.db.tables

import org.example.data.db.config.CustomTable

object Warehouses : CustomTable("warehouses") {
    val name = varchar("name", 255)
    val addressId = reference("address_id", Addresses).nullable()
    val isActive = bool("is_active").default(true)
}