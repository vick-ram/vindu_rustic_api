package org.example.data.db.tables

import org.example.data.db.config.CustomTable
import org.example.utils.tsVector

object Roles : CustomTable("roles") {
    val name = varchar("name", 255).uniqueIndex()
    val description = varchar("description", 255).nullable()
    val tsv = tsVector("tsv")
}