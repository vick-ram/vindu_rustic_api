package org.example.data.db.tables

import kotlinx.serialization.json.Json
import org.example.data.db.config.CustomTable
import org.jetbrains.exposed.v1.json.jsonb

object Settings : CustomTable("settings") {
    val key = varchar("key", 255).uniqueIndex()
    val value = jsonb<Map<String, Any>>("value", Json)
    val description = text("description").nullable()
}