package org.example.services

import org.example.data.cache.OrderItemCache
import org.example.data.repo.OrderItemWithProduct
import org.example.data.repo.TopSellingProduct
import org.example.di.Component
import org.example.di.Inject
import org.example.domain.models.sales.OrderItem
import java.time.OffsetDateTime

@Component
class OrderItemService @Inject constructor(private val cache: OrderItemCache) {
    suspend fun createOrderItem(item: OrderItem): OrderItem = cache.create(item)
    suspend fun getOrderItem(id: String): OrderItem? = cache.read(id)
    suspend fun updateOrderItem(id: String, item: OrderItem): OrderItem? = cache.update(id, item)
    suspend fun deleteOrderItem(id: String): Boolean = cache.delete(id)
    suspend fun createOrderItems(items: List<OrderItem>): List<OrderItem> = cache.bulkCreate(items)
    suspend fun getOrderItems(orderId: String): List<OrderItem> = cache.findByOrderId(orderId)
    suspend fun getOrderItemsWithProductDetails(orderId: String): List<OrderItemWithProduct> = cache.findByOrderIdWithProductDetails(orderId)
    suspend fun getOrderItemsByProduct(productId: String, offset: Int = 0, limit: Int = 20) = cache.findByProductId(productId, offset, limit)
    suspend fun getOrderItemsByVariant(variantId: String, offset: Int = 0, limit: Int = 20) = cache.findByVariantId(variantId, offset, limit)
    suspend fun getTopSellingProducts(limit: Int = 10, startDate: OffsetDateTime? = null, endDate: OffsetDateTime? = null): List<TopSellingProduct> = cache.getTopSellingProducts(limit, startDate, endDate)
}
