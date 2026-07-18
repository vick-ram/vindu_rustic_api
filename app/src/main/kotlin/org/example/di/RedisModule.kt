package org.example.di

import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.RedisClient
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.coroutines
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands
import org.example.config.AppConfig
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single
import org.koin.dsl.module

@OptIn(ExperimentalLettuceCoroutinesApi::class)
val redisModule = module {
    single {
        val config = get<AppConfig>().redis
        RedisClient.create("redis://${config.host}:${config.port}/${config.database}")
    }

    single<StatefulRedisConnection<String, String>> {
        get<RedisClient>().connect()
    }

    single {
        get<StatefulRedisConnection<String, String>>().coroutines()
    }
}