package org.example.data.repo

import org.example.data.db.entities.DiscountEntity
import org.example.data.db.tables.DiscountTable
import org.example.data.mappers.DiscountMapper
import org.example.domain.models.Discount
import org.example.domain.repo.DiscountRepository
import org.example.utils.suspendTransaction

class DiscountRepositoryImpl(
    private val discountMapper: DiscountMapper
): CrudRepositoryImpl<DiscountEntity, Discount>(DiscountEntity, Discount::class), DiscountRepository {
    override fun DiscountEntity.toDomain(): Discount {
        return discountMapper.toModel(this)
    }

    override fun Discount.toEntity(entity: DiscountEntity) {
        discountMapper.toEntity(this, entity)
    }

    override fun getId(domain: Discount): String {
        return domain.id
    }

    override suspend fun findByCode(code: String): Discount? = suspendTransaction {
        DiscountEntity.find { DiscountTable.code.eq(code) }
            .singleOrNull()
            ?.toDomain()
    }

    override suspend fun findActiveDiscounts(): List<Discount> = suspendTransaction{
        DiscountEntity.find { DiscountTable.isActive.eq(true) }
            .map { it.toDomain() }
    }

    override suspend fun findDiscountsByProduct(productId: String): List<Discount> = suspendTransaction {
        TODO()
    }

    override suspend fun incrementUses(discountId: String): Boolean = suspendTransaction {
        DiscountEntity.findByIdAndUpdate(discountId) {
            it.currentUses += 1
        }
        return@suspendTransaction true
    }
}