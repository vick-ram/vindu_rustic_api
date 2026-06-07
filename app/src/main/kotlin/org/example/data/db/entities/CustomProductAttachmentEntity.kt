package org.example.data.db.entities

import org.example.data.db.config.CustomEntity
import org.example.data.db.config.CustomEntityClass
import org.example.data.db.tables.CustomProductAttachments
import org.jetbrains.exposed.v1.core.dao.id.EntityID

class CustomProductAttachmentEntity(id: EntityID<String>) : CustomEntity(id, CustomProductAttachments) {
    companion object : CustomEntityClass<CustomProductAttachmentEntity>(CustomProductAttachments)

    var requestId by CustomProductAttachments.requestId
    var fileUrl by CustomProductAttachments.fileUrl
    var fileType by CustomProductAttachments.fileType
    var fileSizeBytes by CustomProductAttachments.fileSizeBytes
}