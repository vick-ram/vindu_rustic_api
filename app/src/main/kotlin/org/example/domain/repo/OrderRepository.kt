package org.example.domain.repo

import org.example.domain.models.Order

interface OrderRepository: CrudRepository<Order, String> {
    suspend fun createOrder(userId: String): Order
}