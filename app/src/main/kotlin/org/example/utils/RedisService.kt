package org.example.utils

import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.RedisClient
import io.lettuce.core.api.coroutines
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands

@OptIn(ExperimentalLettuceCoroutinesApi::class)
object RedisService {
    private val client = RedisClient.create("redis://localhost:6379")
    private val connection = client.connect().coroutines()
    val commands : RedisCoroutinesCommands<String, String> = connection
}