package org.example.data.mappers

import org.example.data.db.entities.PageEntity
import org.example.data.db.tables.Users
import org.example.domain.models.content.Page
import org.example.domain.repo.EntityMapper
import org.jetbrains.exposed.v1.core.dao.id.EntityID

object PageMapper : EntityMapper<PageEntity, Page, String> {
    override fun toModel(entity: PageEntity): Page {
        return Page(
            id = entity.id.value,
            title = entity.title,
            slug = entity.slug,
            content = entity.content,
            metaTitle = entity.metaTitle,
            metaDescription = entity.metaDescription,
            status = entity.status,
            createdBy = entity.createdBy?.value,
            publishedAt = entity.publishedAt,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }

    override fun toEntity(model: Page, entity: PageEntity): PageEntity {
        entity.title = model.title
        entity.slug = model.slug
        entity.content = model.content
        entity.metaTitle = model.metaTitle
        entity.metaDescription = model.metaDescription
        entity.status = model.status
        entity.createdBy = model.createdBy?.let { EntityID(it, Users) }
        entity.publishedAt = model.publishedAt
        return entity
    }
}