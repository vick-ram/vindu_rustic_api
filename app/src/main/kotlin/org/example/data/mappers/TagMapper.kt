package org.example.data.mappers

import org.example.data.db.entities.TagEntity
import org.example.domain.models.catalog.Tag
import org.example.domain.repo.EntityMapper

object TagMapper : EntityMapper<TagEntity, Tag, String> {
    override fun toModel(entity: TagEntity): Tag {
        return Tag(
            id = entity.id.value,
            name = entity.name,
            slug = entity.slug,
            createdAt = entity.createdAt,
        )
    }

    override fun toEntity(model: Tag, entity: TagEntity): TagEntity {
        entity.name = model.name
        entity.slug = model.slug
        return entity
    }
}