package org.example.data.mappers

import org.example.data.db.entities.AddressEntity
import org.example.data.db.tables.Users
import org.example.domain.models.identity.Address
import org.example.domain.repo.EntityMapper
import org.jetbrains.exposed.v1.core.dao.id.EntityID

object AddressMapper : EntityMapper<AddressEntity, Address, String> {
    override fun toModel(entity: AddressEntity): Address {
        return Address(
            id = entity.id.value,
            userId = entity.userId?.value,
            label = entity.label,
            recipientName = entity.recipientName,
            phoneNumber = entity.phoneNumber,
            countryCode = entity.countryCode,
            country = entity.country,
            city = entity.city,
            postalCode = entity.postalCode,
            addressLine1 = entity.addressLine1,
            addressLine2 = entity.addressLine2,
            latitude = entity.latitude,
            longitude = entity.longitude,
            isDefault = entity.isDefault,
            createdAt = entity.createdAt,
        )
    }

    override fun toEntity(model: Address, entity: AddressEntity): AddressEntity {
        entity.userId = model.userId?.let { EntityID(it, Users) }
        entity.label = model.label
        entity.recipientName = model.recipientName
        entity.phoneNumber = model.phoneNumber
        entity.countryCode = model.countryCode
        entity.country = model.country
        entity.city = model.city
        entity.postalCode = model.postalCode
        entity.addressLine1 = model.addressLine1
        entity.addressLine2 = model.addressLine2
        entity.latitude = model.latitude
        entity.longitude = model.longitude
        entity.isDefault = model.isDefault
        return entity
    }
}