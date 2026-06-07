package org.example.data.repo

import org.example.data.db.entities.ProductVariantEntity
import org.example.data.db.tables.ProductVariants
import org.example.data.mappers.ProductVariantMapper
import org.example.domain.models.catalog.ProductVariant
import org.example.domain.repo.ProductVariantRepository
import org.example.plugins.NotFoundException
import org.example.utils.suspendTransaction
import org.jetbrains.exposed.v1.core.and

class ProductVariantRepositoryImpl(private val variantMapper: ProductVariantMapper) :
    CrudRepositoryImpl<ProductVariantEntity, ProductVariant>(ProductVariantEntity, ProductVariant::class),
    ProductVariantRepository {

    override suspend fun findByProductId(productId: String): List<ProductVariant> = suspendTransaction {
        ProductVariantEntity.find { ProductVariants.productId eq productId }
            .map { it.toDomain() }
    }

    override suspend fun findBySku(sku: String): ProductVariant? = suspendTransaction {
        ProductVariantEntity.find { ProductVariants.sku eq sku }
            .firstOrNull()
            ?.toDomain()
    }

    override suspend fun findActiveByProductId(productId: String): List<ProductVariant> = suspendTransaction {
        ProductVariantEntity.find {
            (ProductVariants.productId eq productId) and (ProductVariants.isActive eq true)
        }.map { it.toDomain() }
    }

    override suspend fun updateStock(variantId: String, quantity: Int): Boolean = suspendTransaction {
        val variant = ProductVariantEntity.findById(variantId)
            ?: throw NotFoundException("Variant not found")
        variant.quantityInStock = quantity
        true
    }

    override fun ProductVariantEntity.toDomain(): ProductVariant = variantMapper.toModel(this)
    override fun ProductVariant.toEntity(entity: ProductVariantEntity) {
        variantMapper.toEntity(this, entity)
    }
    override fun getId(domain: ProductVariant): String = domain.id
}