package org.example.data.repo

import org.example.data.db.entities.TagEntity
import org.example.data.db.tables.ProductTags
import org.example.data.db.tables.Products
import org.example.data.db.tables.Tags
import org.example.data.mappers.TagMapper
import org.example.domain.models.catalog.ProductTag
import org.example.domain.models.catalog.Tag
import org.example.domain.repo.TagRepository
import org.example.plugins.AlreadyExistsException
import org.example.utils.suspendTransaction
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.eq
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.lowerCase
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll

class TagRepositoryImpl(private val tagMapper: TagMapper) :
    CrudRepositoryImpl<TagEntity, Tag>(TagEntity, Tag::class),
    TagRepository {

    override suspend fun findBySlug(slug: String): Tag? = suspendTransaction {
        TagEntity.find { Tags.slug eq slug }
            .firstOrNull()
            ?.toDomain()
    }

    override suspend fun findProductTags(productId: String): List<Tag> = suspendTransaction {
        TagEntity.find {
            Tags.id inSubQuery ProductTags
                .select(ProductTags.tagId)
                .where { ProductTags.productId eq productId }
        }.map { it.toDomain() }
    }

    override suspend fun searchTags(query: String): List<Tag> = suspendTransaction {
        TagEntity.find {
            Tags.name.lowerCase() like "%${query.lowercase()}%"
        }.map { it.toDomain() }
    }

    override suspend fun attachTag(productId: String, tagId: String): ProductTag = suspendTransaction {
        val exists = ProductTags.selectAll().where {
            (ProductTags.productId eq productId) and (ProductTags.tagId eq tagId)
        }.count() > 0

        if (exists) throw AlreadyExistsException("Product tag already exists")

        ProductTags.insert {
            it[ProductTags.productId] = EntityID(productId, Products)
            it[ProductTags.tagId] = EntityID(tagId, Tags)
        }

        ProductTag(productId = productId, tagId = tagId)
    }

    override suspend fun detachTag(productId: String, tagId: String): Boolean = suspendTransaction {
        ProductTags.deleteWhere {
            (ProductTags.productId eq productId) and (ProductTags.tagId eq tagId)
        } > 0
    }

    override fun TagEntity.toDomain(): Tag = tagMapper.toModel(this)
    override fun Tag.toEntity(entity: TagEntity) {
        tagMapper.toEntity(this, entity)
    }

    override fun getId(domain: Tag): String = domain.id
}
