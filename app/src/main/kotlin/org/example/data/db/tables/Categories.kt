package org.example.data.db.tables

import org.example.data.db.config.CustomTable

object Categories: CustomTable("categories") {
    val parentId = reference("parent_id", this).nullable()
    val name = varchar("name", 255)
    val slug = varchar("slug", 255).uniqueIndex()
    val description = text("description").nullable()
    val imageUrl = text("image_url").nullable()
    val isActive = bool("is_active").default(true)
    val sortOrder = integer("sort_order").default(0)
}