package org.example.data.db.tables

import org.example.data.db.config.CustomTable
import org.jetbrains.exposed.v1.datetime.timestampWithTimeZone

object Pages : CustomTable("pages") {
    val title = varchar("title", 255)
    val slug = varchar("slug", 255).uniqueIndex()
    val content = text("content").nullable()
    val metaTitle = varchar("meta_title", 255).nullable()
    val metaDescription = text("meta_description").nullable()
    val status = varchar("status", 50).default("DRAFT")
    val createdBy = reference("created_by", Users).nullable()
    val publishedAt = timestampWithTimeZone("published_at").nullable()
}