package org.example.services

import org.example.data.cache.OrderStatusHistoryCache
import org.example.data.repo.StatusDuration
import org.example.di.Component
import org.example.di.Inject
import org.example.domain.models.sales.OrderStatusHistory

@Component
class OrderStatusHistoryService @Inject constructor(private val cache: OrderStatusHistoryCache) {
    suspend fun logStatusChange(orderId: String, oldStatus: String?, newStatus: String, changedBy: String? = null, comment: String? = null): OrderStatusHistory =
        cache.logStatusChange(orderId, oldStatus, newStatus, changedBy, comment)
    suspend fun getStatusHistory(orderId: String): List<OrderStatusHistory> = cache.findByOrderId(orderId)
    suspend fun getLatestStatusChange(orderId: String): OrderStatusHistory? = cache.getLatestStatusChange(orderId)
    suspend fun getStatusDuration(orderId: String): List<StatusDuration> = cache.getStatusDuration(orderId)
    suspend fun getStatusChangesByUser(changedBy: String, offset: Int = 0, limit: Int = 50) = cache.findByChangedBy(changedBy, offset, limit)
}
