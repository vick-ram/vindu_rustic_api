package org.example.data.db.tables

import org.example.data.db.config.CustomTable

object Tags : CustomTable("tags") {
    val name = varchar("name", 100).uniqueIndex()
    val slug = varchar("slug", 120).uniqueIndex()
}