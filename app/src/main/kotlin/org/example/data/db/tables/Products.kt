package org.example.data.db.tables

import org.example.data.db.config.CustomTable
import org.example.utils.tsVector
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.datetime.datetime
import org.jetbrains.exposed.v1.datetime.timestampWithTimeZone


object Products : CustomTable("products") {
    val categoryId = reference("category_id", Categories).nullable()
    val title = varchar("title", 255)
    val slug = varchar("slug", 255).uniqueIndex()
    val shortDescription = text("short_description").nullable()
    val description = text("description").nullable()
    val status = varchar("status", 50).default("DRAFT")
    val productType = varchar("product_type", 50).default("STANDARD")
    val brand = varchar("brand", 255).nullable()
    val isCustomizable = bool("is_customizable").default(false)
    val isFeatured = bool("is_featured").default(false)
    val seoTitle = varchar("seo_title", 255).nullable()
    val seoDescription = text("seo_description").nullable()
    val deletedAt = timestampWithTimeZone("deleted_at").nullable()
    val tsv = tsVector("tsv")
}

