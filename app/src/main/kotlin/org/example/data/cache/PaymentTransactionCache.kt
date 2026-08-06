package org.example.data.cache

import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.builtins.ListSerializer
import org.example.data.repo.CacheConfig
import org.example.data.repo.CrudCache
import org.example.data.repo.PaymentTransactionRepository
import org.example.data.repo.TransactionStats
import org.example.di.Component
import org.example.di.Inject
import org.example.domain.models.payments.PaymentTransaction
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.time.OffsetDateTime

@OptIn(ExperimentalLettuceCoroutinesApi::class)
@Component
class PaymentTransactionCache @Inject constructor(
    private val redis: RedisCoroutinesCommands<String, String>,
    private val repository: PaymentTransactionRepository,
) : CrudCache<PaymentTransaction, String>(
    redis = redis,
    delegate = repository,
    getId = { it.id },
    serializer = PaymentTransaction.serializer(),
    config = object : CacheConfig {
        override val cacheName = "payment_transactions"
        override val ttl = 1800L // 30-minute short lifecycle TTL for fast-moving transaction steps
    }
) {
    private val logger: Logger = LoggerFactory.getLogger(PaymentTransactionCache::class.java)
    private val txListSerializer = ListSerializer(PaymentTransaction.serializer())

    /**
     * Clear active lookups and history logs associated with a dynamic target payment tracking block.
     */
    private suspend fun invalidatePaymentCaches(paymentId: String, providerTxId: String?) {
        redis.del("${config.cacheName}:payment:$paymentId")
        redis.del("${config.cacheName}:latest:$paymentId")
        if (providerTxId != null) {
            redis.del("${config.cacheName}:provider_tx:$providerTxId")
            redis.del("${config.cacheName}:is_duplicate:$providerTxId")
        }
        invalidateCollectionCaches()
    }

    /**
     * Mutates transaction state from external payment processors, synchronizing secondary key maps.
     */
    suspend fun updateTransactionStatus(
        id: String,
        status: String,
        providerResponse: Map<String, Any>? = null,
        errorMessage: String? = null
    ): PaymentTransaction? {
        val updatedTx = repository.updateTransactionStatus(id, status, providerResponse, errorMessage)
        if (updatedTx != null) {
            putInCache(updatedTx.id, updatedTx)
            invalidatePaymentCaches(updatedTx.paymentId, updatedTx.providerTransactionId)
        }
        return updatedTx
    }

    /**
     * Retrieves all ledger attempts made against a specific payment entry.
     */
    suspend fun findByPaymentId(paymentId: String): List<PaymentTransaction> {
        val cacheKey = "${config.cacheName}:payment:$paymentId"
        val cachedJson = redis.get(cacheKey)

        if (cachedJson != null) {
            try {
                return Json.decodeFromString(txListSerializer, cachedJson)
            } catch (e: Exception) {
                logger.error("Failed to deserialize transaction timeline cache for payment $paymentId", e)
            }
        }

        val freshTransactions = repository.findByPaymentId(paymentId)
        if (freshTransactions.isNotEmpty()) {
            try {
                redis.setex(cacheKey, config.ttl ?: 1800L, Json.encodeToString(txListSerializer, freshTransactions))
            } catch (e: Exception) {
                logger.error("Failed to write transaction timeline cache for payment $paymentId", e)
            }
        }
        return freshTransactions
    }

    /**
     * Resolves unique payment tracking blocks via external gateway identifiers (crucial for webhook ingest).
     */
    suspend fun findByProviderTransactionId(providerTransactionId: String): PaymentTransaction? {
        val cacheKey = "${config.cacheName}:provider_tx:$providerTransactionId"
        val cachedJson = redis.get(cacheKey)

        if (cachedJson != null) {
            try {
                return Json.decodeFromString(PaymentTransaction.serializer(), cachedJson)
            } catch (e: Exception) {
                logger.error("Failed to deserialize transaction cache for provider token $providerTransactionId", e)
            }
        }

        val freshTx = repository.findByProviderTransactionId(providerTransactionId)
        if (freshTx != null) {
            try {
                redis.setex(
                    cacheKey,
                    config.ttl ?: 1800L,
                    Json.encodeToString(PaymentTransaction.serializer(), freshTx)
                )
            } catch (e: Exception) {
                logger.error("Failed to write transaction cache for provider token $providerTransactionId", e)
            }
        }
        return freshTx
    }

    /**
     * Fast retrieval for checkout pages determining the absolute newest status attempt.
     */
    suspend fun getLatestTransaction(paymentId: String): PaymentTransaction? {
        val cacheKey = "${config.cacheName}:latest:$paymentId"
        val cachedJson = redis.get(cacheKey)

        if (cachedJson != null) {
            try {
                return Json.decodeFromString(PaymentTransaction.serializer(), cachedJson)
            } catch (e: Exception) {
                logger.error("Failed to deserialize latest transaction cache for payment $paymentId", e)
            }
        }

        val freshTx = repository.getLatestTransaction(paymentId)
        if (freshTx != null) {
            try {
                redis.setex(
                    cacheKey,
                    config.ttl ?: 1800L,
                    Json.encodeToString(PaymentTransaction.serializer(), freshTx)
                )
            } catch (e: Exception) {
                logger.error("Failed to write latest transaction cache for payment $paymentId", e)
            }
        }
        return freshTx
    }

    /**
     * Safeguards asynchronous webhooks against network double-retries using scalar text values.
     */
    suspend fun isDuplicateTransaction(providerTransactionId: String): Boolean {
        val cacheKey = "${config.cacheName}:is_duplicate:$providerTransactionId"
        val cachedValue = redis.get(cacheKey)

        if (cachedValue != null) {
            return cachedValue.toBoolean()
        }

        val isDuplicate = repository.isDuplicateTransaction(providerTransactionId)
        try {
            // Note: If false, cache short-term to allow subsequent retries if the first failed mid-flight
            redis.setex(cacheKey, config.ttl ?: 1800L, isDuplicate.toString())
        } catch (e: Exception) {
            logger.error("Failed to write duplicate filter log for provider token $providerTransactionId", e)
        }
        return isDuplicate
    }

    // --- Passthrough Channels (Bypassing Redis due to unbound ranges or high churn) ---

    suspend fun findByTransactionType(
        transactionType: String,
        offset: Int = 0,
        limit: Int = 50
    ): List<PaymentTransaction> {
        return repository.findByTransactionType(transactionType, offset, limit)
    }

    suspend fun findFailedTransactions(offset: Int = 0, limit: Int = 50): List<PaymentTransaction> {
        return repository.findFailedTransactions(offset, limit)
    }

    suspend fun getTransactionStats(
        startDate: OffsetDateTime,
        endDate: OffsetDateTime,
        transactionType: String? = null
    ): TransactionStats {
        return repository.getTransactionStats(startDate, endDate, transactionType)
    }
}