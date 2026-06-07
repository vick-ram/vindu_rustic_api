package org.example.data.mappers

import org.example.data.db.entities.SettingsEntity
import org.example.domain.models.content.Setting
import org.example.domain.repo.EntityMapper

object SettingsMapper : EntityMapper<SettingsEntity, Setting, String> {
    override fun toModel(entity: SettingsEntity): Setting {
        return Setting(
            id = entity.id.value,
            key = entity.key,
            value = entity.value,
            description = entity.description,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }

    override fun toEntity(model: Setting, entity: SettingsEntity): SettingsEntity {
        entity.key = model.key
        entity.value = model.value
        entity.description = model.description
        return entity
    }
}