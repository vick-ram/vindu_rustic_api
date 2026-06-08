package org.example.data.db.tables

import org.example.data.db.config.CustomTable

object Addresses: CustomTable("addresses")  {
    val userId = reference("user_id", Users).nullable()
    val label = varchar("label", 100).nullable()
    val recipientName = varchar("recipient_name", 255)
    val phoneNumber = varchar("phone_number", 30)
    val countryCode = varchar("country_code", 10).default("KE")
    val country = varchar("country", 120).default("Kenya")
    val city = varchar("city", 120).nullable()
    val stateProvince = varchar("state_province", 120).nullable()
    val postalCode = varchar("postal_code", 30).nullable()
    val addressLine1 = text("address_line1")
    val addressLine2 = text("address_line2").nullable()
    val latitude = decimal("latitude", 10, 7).nullable()
    val longitude = decimal("longitude", 10, 7).nullable()
    val isDefault = bool("is_default").default(false)
}