package org.example.services

import org.example.data.cache.CustomProductQuoteCache
import org.example.di.Inject
import org.example.domain.models.customization.CustomProductQuote
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.time.OffsetDateTime

class CustomProductQuoteService @Inject constructor(
    private val quoteCache: CustomProductQuoteCache
) {
    companion object {
        private val logger = LoggerFactory.getLogger(CustomProductQuoteService::class.java)
    }

    private val actionableStatuses = setOf("sent", "viewed")

    suspend fun getQuote(quoteId: String): CustomProductQuote? {
        return quoteCache.read(quoteId) // Uses parent's cached read
    }

    suspend fun getRequestQuotes(requestId: String): List<CustomProductQuote> {
        return quoteCache.findByRequestId(requestId)
    }

    suspend fun getLatestQuote(requestId: String): CustomProductQuote? {
        return quoteCache.getLatestQuote(requestId)
    }

    suspend fun getValidQuotes(requestId: String): List<CustomProductQuote> {
        return quoteCache.getValidQuotes(requestId)
    }

    suspend fun acceptQuote(quoteId: String): CustomProductQuote? {
        val existing = quoteCache.read(quoteId) ?: run {
            logger.warn("Quote not found: $quoteId")
            return null
        }

        if (existing.status !in actionableStatuses) {
            logger.warn("Quote $quoteId cannot be accepted. Current status: ${existing.status}")
            return null
        }

        if (existing.validUntil != null && existing.validUntil.isBefore(OffsetDateTime.now())) {
            logger.warn("Quote $quoteId has expired")
            return null
        }

        logger.info("Accepting quote: $quoteId")
        return quoteCache.acceptQuote(quoteId)
    }

    suspend fun rejectQuote(quoteId: String, reason: String? = null): CustomProductQuote? {
        val existing = quoteCache.read(quoteId) ?: run {
            logger.warn("Quote not found: $quoteId")
            return null
        }

        if (existing.status !in actionableStatuses) {
            logger.warn("Quote $quoteId cannot be rejected. Current status: ${existing.status}")
            return null
        }

        logger.info("Rejecting quote: $quoteId, reason: $reason")
        return quoteCache.rejectQuote(quoteId, reason)
    }

    suspend fun viewQuote(quoteId: String): CustomProductQuote? {
        return quoteCache.markAsViewed(quoteId)
    }

    suspend fun updateQuote(
        quoteId: String,
        price: BigDecimal?,
        currency: String?,
        validUntil: OffsetDateTime?,
        description: String?
    ): CustomProductQuote? {
        val existing = quoteCache.read(quoteId) ?: run {
            logger.warn("Quote not found: $quoteId")
            return null
        }

        if (existing.status != "draft" && existing.status != "sent") {
            logger.warn("Quote $quoteId cannot be updated. Current status: ${existing.status}")
            return null
        }

        val updatedQuote = existing.copy(
            quotedPrice = price ?: existing.quotedPrice,
            currency = currency ?: existing.currency,
            validUntil = validUntil ?: existing.validUntil,
            description = description ?: existing.description
        )

        logger.info("Updating quote: $quoteId")
        return quoteCache.update(quoteId, updatedQuote) // Uses parent's update
    }

    suspend fun deleteQuote(quoteId: String): Boolean {
        val existing = quoteCache.read(quoteId) ?: run {
            logger.warn("Quote not found: $quoteId")
            return false
        }

        if (existing.status == "accepted") {
            logger.warn("Cannot delete accepted quote: $quoteId")
            return false
        }

        logger.info("Deleting quote: $quoteId")
        return quoteCache.delete(quoteId) // Uses parent's delete
    }

    suspend fun getQuotesByStatus(
        status: String,
        offset: Int = 0,
        limit: Int = 20
    ): List<CustomProductQuote> {
        return quoteCache.findByStatus(status, offset, limit)
    }

    suspend fun getQuotesByCreator(
        createdBy: String,
        offset: Int = 0,
        limit: Int = 20
    ): List<CustomProductQuote> {
        return quoteCache.findByCreator(createdBy, offset, limit)
    }

    suspend fun expireOldQuotes(): Int {
        logger.info("Expiring old quotes")
        val expiredCount = quoteCache.expireOldQuotes()
        logger.info("Expired $expiredCount quotes")
        return expiredCount
    }

    suspend fun sendQuote(quoteId: String): CustomProductQuote? {
        val existing = quoteCache.read(quoteId) ?: run {
            logger.warn("Quote not found: $quoteId")
            return null
        }

        if (existing.status != "draft") {
            logger.warn("Only draft quotes can be sent. Current status: ${existing.status}")
            return null
        }

        val updatedQuote = existing.copy(
            status = "sent"
        )

        logger.info("Sending quote: $quoteId")
        return quoteCache.update(quoteId, updatedQuote)
    }

    suspend fun getQuoteStats(requestId: String): QuoteStats {
        val quotes = quoteCache.findByRequestId(requestId)

        return QuoteStats(
            totalQuotes = quotes.size,
            validQuotes = quotes.count { it.status in actionableStatuses },
            acceptedQuote = quotes.firstOrNull { it.status == "accepted" },
            rejectedQuotes = quotes.count { it.status == "rejected" },
            expiredQuotes = quotes.count { it.status == "expired" }
        )
    }
}

data class QuoteStats(
    val totalQuotes: Int,
    val validQuotes: Int,
    val acceptedQuote: CustomProductQuote?,
    val rejectedQuotes: Int,
    val expiredQuotes: Int
)