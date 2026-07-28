package org.example.data.cache

import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.example.data.repo.CacheConfig
import org.example.data.repo.CrudCache
import org.example.data.repo.PaymentRepository
import org.example.data.repo.PaymentSummary
import org.example.di.Component
import org.example.di.Inject
import org.example.domain.models.payments.Payment
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.time.OffsetDateTime

@Component
@OptIn(ExperimentalLettuceCoroutinesApi::class)
class PaymentCache @Inject constructor(
    private val redis: RedisCoroutinesCommands<String, String>,
    private val repository: PaymentRepository,
    serializer: KSerializer<Payment>
) : CrudCache<Payment, String>(
    redis = redis,
    delegate = repository,
    getId = { it.id },
    serializer = serializer,
    config = object : CacheConfig {
        override val cacheName = "payments"
        override val ttl = 3600L
    }
) {
    private val logger: Logger = LoggerFactory.getLogger(PaymentCache::class.java)
    private val paymentListSerializer = ListSerializer(Payment.serializer())

    /**
     * Helper to clear order-linked caches when a payment row undergoes a lifecycle update.
     */
    private suspend fun invalidateOrderCaches(orderId: String) {
        redis.del("${config.cacheName}:order:$orderId")
        redis.del("${config.cacheName}:total_paid:$orderId")
        redis.del("${config.cacheName}:has_completed:$orderId")
        invalidateCollectionCaches()
    }

    /**
     * Updates payment status, synchronizes the single entity cache entry,
     * and purges any downstream order counters.
     */
    suspend fun updateStatus(id: String, status: String): Payment? {
        val updatedPayment = repository.updateStatus(id, status)
        if (updatedPayment != null) {
            putInCache(updatedPayment.id, updatedPayment)
            invalidateOrderCaches(updatedPayment.orderId)
        }
        return updatedPayment
    }

    /**
     * Cancels a payment transaction and updates local indexes.
     */
    suspend fun cancelPayment(id: String): Payment? {
        val cancelledPayment = repository.cancelPayment(id)
        if (cancelledPayment != null) {
            putInCache(cancelledPayment.id, cancelledPayment)
            invalidateOrderCaches(cancelledPayment.orderId)
        }
        return cancelledPayment
    }

    /**
     * Caches all payments belonging to a specific order payload.
     */
    suspend fun findByOrderId(orderId: String): List<Payment> {
        val cacheKey = "${config.cacheName}:order:$orderId"
        val cachedJson = redis.get(cacheKey)

        if (cachedJson != null) {
            try {
                return Json.decodeFromString(paymentListSerializer, cachedJson)
            } catch (e: Exception) {
                logger.error("Failed to deserialize payment list cache for order $orderId", e)
            }
        }

        val freshPayments = repository.findByOrderId(orderId)
        if (freshPayments.isNotEmpty()) {
            try {
                redis.setex(cacheKey, config.ttl ?: 3600L, Json.encodeToString(paymentListSerializer, freshPayments))
            } catch (e: Exception) {
                logger.error("Failed to write payment list cache for order $orderId", e)
            }
        }
        return freshPayments
    }

    /**
     * Caches the total financial amount successfully processed against a single order.
     */
    suspend fun getTotalPaidForOrder(orderId: String): BigDecimal {
        val cacheKey = "${config.cacheName}:total_paid:$orderId"
        val cachedValue = redis.get(cacheKey)

        if (cachedValue != null) {
            return BigDecimal(cachedValue)
        }

        val freshTotal = repository.getTotalPaidForOrder(orderId)
        try {
            redis.setex(cacheKey, config.ttl ?: 3600L, freshTotal.toPlainString())
        } catch (e: Exception) {
            logger.error("Failed to cache total paid counter for order $orderId", e)
        }
        return freshTotal
    }

    /**
     * Caches boolean lookup flags checking for completed transactions.
     */
    suspend fun hasCompletedPayment(orderId: String): Boolean {
        val cacheKey = "${config.cacheName}:has_completed:$orderId"
        val cachedValue = redis.get(cacheKey)

        if (cachedValue != null) {
            return cachedValue.toBoolean()
        }

        val hasCompleted = repository.hasCompletedPayment(orderId)
        try {
            redis.setex(cacheKey, config.ttl ?: 3600L, hasCompleted.toString())
        } catch (e: Exception) {
            logger.error("Failed to cache complete verification status for order $orderId", e)
        }
        return hasCompleted
    }

    suspend fun findByStatus(status: String, offset: Int = 0, limit: Int = 20): List<Payment> {
        return repository.findByStatus(status, offset, limit)
    }

    suspend fun findByProvider(provider: String, offset: Int = 0, limit: Int = 20): List<Payment> {
        return repository.findByProvider(provider, offset, limit)
    }

    suspend fun getPaymentSummary(
        startDate: OffsetDateTime,
        endDate: OffsetDateTime,
        provider: String? = null
    ): PaymentSummary {
        return repository.getPaymentSummary(startDate, endDate, provider)
    }

    suspend fun findByDateRange(
        startDate: OffsetDateTime,
        endDate: OffsetDateTime,
        status: String? = null,
        offset: Int = 0,
        limit: Int = 50
    ): List<Payment> {
        return repository.findByDateRange(startDate, endDate, status, offset, limit)
    }
}