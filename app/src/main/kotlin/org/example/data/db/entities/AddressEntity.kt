package org.example.data.db.entities

import org.example.data.db.config.CustomEntity
import org.example.data.db.config.CustomEntityClass
import org.example.data.db.tables.Addresses
import org.jetbrains.exposed.v1.core.dao.id.EntityID

class AddressEntity(id: EntityID<String>): CustomEntity(id, Addresses) {

    companion object : CustomEntityClass<AddressEntity>(Addresses)

    var userId by Addresses.userId
    var label by Addresses.label
    var recipientName by Addresses.recipientName
    var phoneNumber by Addresses.phoneNumber
    var countryCode by Addresses.countryCode
    var country by Addresses.country
    var city by Addresses.city
    var postalCode by Addresses.postalCode
    var addressLine1 by Addresses.addressLine1
    var addressLine2 by Addresses.addressLine2
    var latitude by Addresses.latitude
    var longitude by Addresses.longitude
    var isDefault by Addresses.isDefault
}