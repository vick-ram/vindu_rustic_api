package org.example.di

import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.RedisClient
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.async.RedisAsyncCommands
import io.lettuce.core.api.coroutines
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands
import org.koin.dsl.module

@OptIn(ExperimentalLettuceCoroutinesApi::class)
val redisModule = module {
    // Singleton redis client
    single {
        RedisClient.create("redis://localhost:6379")
    }

    // Singleton connection
    single<StatefulRedisConnection<String, String>> {
        get<RedisClient>().connect()
    }

    // Sync commands
    single<RedisCoroutinesCommands<String, String>> {
        get<StatefulRedisConnection<String, String>>().coroutines()
    }
}