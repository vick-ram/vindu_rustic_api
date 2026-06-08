package org.example.celery

import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import org.example.utils.Json
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalLettuceCoroutinesApi::class)
class RedisBroker(
    private val redis: RedisCoroutinesCommands<String, String>,
    private val json: Json,
    private val prefix: String = "celery"
): MessageBroker {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val deliveryTagToQueue = ConcurrentHashMap<String, String>()

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    override suspend fun publish(task: TaskMessage, queue: String, priority: Int) {
        val payload = json.encodeToString(task)
        redis.zadd(queueKey(queue), priority.toDouble(), payload)
        logger.debug("Published task {} to {}", task.id, queue)
    }

    override fun consume(
        queue: String
    ): Flow<BrokerRecord<TaskMessage>> = flow {
        val queueKey = queueKey(queue)
        val processingKey = processingKey(queue)

        while (currentCoroutineContext().isActive) {
            try {
                val results = redis.zpopmin(queueKey, 1)
                val entry = results.firstOrNull()

                if (entry == null) {
                    delay(250.milliseconds)
                    continue
                }

                val deliveryTag = UUID.randomUUID().toString()
                deliveryTagToQueue[deliveryTag] = queue

                redis.hset(processingKey, deliveryTag, entry.value)
                val task = json.decodeFromString<TaskMessage>(entry.value)

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

    override suspend fun acknowledge(deliveryTag: String) {
        val queue = deliveryTagToQueue.remove(deliveryTag) ?: return
        redis.hdel(processingKey(queue), deliveryTag)
    }
    override suspend fun reject(deliveryTag: String, requeue: Boolean) {
        val queue = deliveryTagToQueue.remove(deliveryTag) ?: return
        val processingKey = processingKey(queue)
        val payload = redis.hget(processingKey, deliveryTag)
        redis.hdel(processingKey, deliveryTag)

        if (requeue && payload != null) {
            redis.zadd(queueKey(queue), 0.0, payload)
        }
    }

    override suspend fun close() {
        deliveryTagToQueue.clear()
    }

    private fun queueKey(queue: String) = "$prefix:queue:$queue"

    private fun processingKey(queue: String) = "$prefix:processing:$queue"
}