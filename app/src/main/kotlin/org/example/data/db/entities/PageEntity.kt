package org.example.data.db.entities

import org.example.data.db.config.CustomEntity
import org.example.data.db.config.CustomEntityClass
import org.example.data.db.tables.Pages
import org.jetbrains.exposed.v1.core.dao.id.EntityID

class PageEntity(id: EntityID<String>) : CustomEntity(id, Pages) {
    companion object : CustomEntityClass<PageEntity>(Pages)

    var title by Pages.title
    var slug by Pages.slug
    var content by Pages.content
    var metaTitle by Pages.metaTitle
    var metaDescription by Pages.metaDescription
    var status by Pages.status
    var createdBy by Pages.createdBy
    var publishedAt by Pages.publishedAt
}