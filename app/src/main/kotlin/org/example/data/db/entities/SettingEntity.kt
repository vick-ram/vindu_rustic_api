package org.example.data.db.entities

import org.example.data.db.config.CustomEntity
import org.example.data.db.config.CustomEntityClass
import org.example.data.db.tables.Settings
import org.jetbrains.exposed.v1.core.dao.id.EntityID

class SettingsEntity(id: EntityID<String>) : CustomEntity(id, Settings) {
    companion object : CustomEntityClass<SettingsEntity>(Settings)

    var key by Settings.key
    var value by Settings.value
    var description by Settings.description
}
