package org.example.celery

import kotlinx.coroutines.flow.Flow

interface MessageBroker {
    suspend fun publish(task: TaskMessage, queue: String, priority: Int = 0)
    fun consume(queue: String): Flow<BrokerRecord<TaskMessage>>
    suspend fun acknowledge(deliveryTag: String)
    suspend fun reject(deliveryTag: String, requeue: Boolean = true)
    suspend fun close()
}

data class BrokerRecord<T>(
    val payload: T,
    val deliveryTag: String,
    val queue: String
)