package org.example.data.repo

import org.example.data.db.entities.SpecialOfferEntity
import org.example.data.db.tables.SpecialOfferTable
import org.example.data.mappers.SpecialOfferMapper
import org.example.domain.models.catalog.SpecialOffer
import org.example.domain.repo.SpecialOfferRepository
import org.example.utils.suspendTransaction

class SpecialOfferRepositoryImpl(private val specialOfferMapper: SpecialOfferMapper): CrudRepositoryImpl<SpecialOfferEntity, SpecialOffer>(
    entityClass = SpecialOfferEntity,
    domainClass = SpecialOffer::class
), SpecialOfferRepository {
    override fun SpecialOfferEntity.toDomain(): SpecialOffer {
        return specialOfferMapper.toModel(this)
    }

    override fun SpecialOffer.toEntity(entity: SpecialOfferEntity) {
        specialOfferMapper.toEntity(this, entity)
    }

    override fun getId(domain: SpecialOffer): String {
        return domain.id
    }

    override suspend fun findActiveOffers(): List<SpecialOffer> = suspendTransaction {
        SpecialOfferEntity.find { SpecialOfferTable.isActive.eq(true) }
            .map { it.toDomain() }
    }

    override suspend fun findOffersByProduct(productId: String): List<SpecialOffer> = suspendTransaction {
        TODO("Not yet implemented")
    }
}