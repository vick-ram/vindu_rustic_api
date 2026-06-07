package org.example.data.db.entities

import org.example.data.db.config.CustomEntity
import org.example.data.db.config.CustomEntityClass
import org.example.data.db.tables.CustomProductQuotes
import org.jetbrains.exposed.v1.core.dao.id.EntityID

class CustomProductQuoteEntity(id: EntityID<String>) : CustomEntity(id, CustomProductQuotes) {
    companion object : CustomEntityClass<CustomProductQuoteEntity>(CustomProductQuotes)

    var requestId by CustomProductQuotes.requestId
    var quotedPrice by CustomProductQuotes.quotedPrice
    var currency by CustomProductQuotes.currency
    var productionTimelineDays by CustomProductQuotes.productionTimelineDays
    var description by CustomProductQuotes.description
    var validUntil by CustomProductQuotes.validUntil
    var status by CustomProductQuotes.status
    var createdBy by CustomProductQuotes.createdBy
}