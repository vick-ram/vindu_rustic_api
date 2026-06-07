package org.example.data.db.tables

import org.example.data.db.config.CustomTable

object Wishlists : CustomTable("wishlists") {
    val userId = reference("user_id", Users)
    val name = varchar("name", 255).default("Default")
    val isPublic = bool("is_public").default(false)

    init {
        uniqueIndex(userId, name)
    }
}