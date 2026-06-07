package org.example.data.db.tables

import org.example.data.db.config.CustomTable

object ProductTags : CustomTable("product_tags") {
    val productId = reference("product_id", Products)
    val tagId = reference("tag_id", Tags)

    override val primaryKey = PrimaryKey(productId, tagId)
}