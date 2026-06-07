package org.example.data.mappers

import org.example.data.db.entities.CustomProductAttachmentEntity
import org.example.data.db.tables.CustomProductRequests
import org.example.domain.models.customization.CustomProductAttachment
import org.example.domain.repo.EntityMapper
import org.jetbrains.exposed.v1.core.dao.id.EntityID

object CustomProductAttachmentMapper : EntityMapper<CustomProductAttachmentEntity, CustomProductAttachment, String> {
    override fun toModel(entity: CustomProductAttachmentEntity): CustomProductAttachment {
        return CustomProductAttachment(
            id = entity.id.value,
            requestId = entity.requestId.value,
            fileUrl = entity.fileUrl,
            fileType = entity.fileType,
            fileSize = entity.fileSizeBytes,
            createdAt = entity.createdAt,
        )
    }

    override fun toEntity(model: CustomProductAttachment, entity: CustomProductAttachmentEntity): CustomProductAttachmentEntity {
        entity.requestId = EntityID(model.requestId, CustomProductRequests)
        entity.fileUrl = model.fileUrl
        entity.fileType = model.fileType
        entity.fileSizeBytes = model.fileSize
        return entity
    }
}