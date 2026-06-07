package org.example.data.db.tables

import org.example.data.db.config.CustomTable
import org.example.data.db.config.gsonJsonb

object Settings : CustomTable("settings") {
    val key = varchar("key", 255).uniqueIndex()
    val value = gsonJsonb<Map<String, Any>>("value")
    val description = text("description").nullable()
}