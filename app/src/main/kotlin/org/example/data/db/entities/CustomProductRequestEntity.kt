package org.example.data.db.entities

import org.example.data.db.config.CustomEntity
import org.example.data.db.config.CustomEntityClass
import org.example.data.db.tables.CustomProductAttachments
import org.example.data.db.tables.CustomProductQuotes
import org.example.data.db.tables.CustomProductRequests
import org.jetbrains.exposed.v1.core.dao.id.EntityID

class CustomProductRequestEntity(id: EntityID<String>) : CustomEntity(id, CustomProductRequests) {
    companion object : CustomEntityClass<CustomProductRequestEntity>(CustomProductRequests)

    var userId by CustomProductRequests.userId
    var title by CustomProductRequests.title
    var description by CustomProductRequests.description
    var specifications by CustomProductRequests.specifications
    var estimatedBudgetMin by CustomProductRequests.estimatedBudgetMin
    var estimatedBudgetMax by CustomProductRequests.estimatedBudgetMax
    var status by CustomProductRequests.status
    var notes by CustomProductRequests.notes

    val quotes by CustomProductQuoteEntity referrersOn CustomProductQuotes.requestId
    val attachments by CustomProductAttachmentEntity referrersOn CustomProductAttachments.requestId
}