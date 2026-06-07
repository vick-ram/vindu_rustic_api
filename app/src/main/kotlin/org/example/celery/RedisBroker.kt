package org.example.celery

import com.google.gson.Gson
import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.RedisClient
import io.lettuce.core.ScoredValue
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands
import io.lettuce.core.pubsub.RedisPubSubAdapter
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.forEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.example.utils.Json
import org.example.utils.RedisService
import org.slf4j.LoggerFactory
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalLettuceCoroutinesApi::class)
class RedisBroker(
    private val redis: RedisCoroutinesCommands<String, String>,
    private val json: Json,
    private val prefix: String = "celery"
): MessageBroker {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    override suspend fun publish(task: TaskMessage, queue: String, priority: Int) {
        val queueKey = queueKey(queue)
        val payload = json.encodeToString(task)

        redis.zadd(queueKey,  priority.toDouble(), payload)

        logger.debug("Published task {} to {}", task.id, queue)

    }

    override fun consume(
        queue: String
    ): Flow<BrokerRecord<TaskMessage>> = flow {
        val queueKey = queueKey(queue)
        val processingKey = processingKey(queue)

        while (currentCoroutineContext().isActive) {
            try {
                val entry = popNext(queueKey)

                if (entry == null) {
                    delay(250.milliseconds)
                    continue
                }

                val payload = entry.value
                val deliveryTag = UUID.randomUUID().toString()

                redis.hset(processingKey, deliveryTag, payload)
                val task = json.decodeFromString<TaskMessage>(payload)

                emit(
                    BrokerRecord(
                        payload = task,
                        deliveryTag = deliveryTag,
                        queue = queue
                    )
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.error("Redis consume error", e)
                delay(1000.milliseconds)
            }
        }
    }

    private suspend fun popNext(queueKey: String): ScoredValue<String>? {
        val results = redis.zpopmin(queueKey, 1)
        return results.firstOrNull()
    }

    override suspend fun acknowledge(deliveryTag: String) {
        removeFromProcessing(deliveryTag)
    }

    override suspend fun reject(deliveryTag: String, requeue: Boolean) {
        redis.keys("$prefix:processing:*").collect { key ->
            val payload = redis.hget(key, deliveryTag)
            redis.hdel(key, deliveryTag)

            if (requeue) {
                val queue = key.substringAfterLast(":")
                payload?.let { redis.zadd(queueKey(queue), 0.0, it) }
            }
        }
    }

    override suspend fun getQueueLength(queue: String): Long {
        return redis.zcard(queueKey(queue)) ?: 0
    }

    override suspend fun purgeQueue(queue: String) {
        redis.del(queueKey(queue))
        redis.del(processingKey(queue))
    }

    override suspend fun close() {
        scope.cancel()
    }

    private suspend fun removeFromProcessing(deliveryTag: String) {
        redis.keys("$prefix:processing:*").collect { key ->
            redis.hdel(key, deliveryTag)
        }
    }

    private fun queueKey(queue: String) = "$prefix:queue:$queue"

    private fun processingKey(queue: String) = "$prefix:processing:$queue"
}