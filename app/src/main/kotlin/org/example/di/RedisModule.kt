package org.example.di

import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.RedisClient
import io.lettuce.core.RedisURI
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.coroutines
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands
import io.lettuce.core.codec.StringCodec
import org.example.config.AppConfig
import org.koin.dsl.module

@OptIn(ExperimentalLettuceCoroutinesApi::class)
val redisModule = module {
    single {
        val config = get<AppConfig>().redis
        val uri = RedisURI.builder()
            .withHost(config.host)
            .withPort(config.port)
            .withDatabase(config.database)
            .build()
//        RedisClient.create("redis://${config.host}:${config.port}/${config.database}")
        RedisClient.create(uri)
    }

    single<StatefulRedisConnection<String, String>> {
        get<RedisClient>().connect(StringCodec.UTF8)
    }

    single<RedisCoroutinesCommands<String, String>> {
        get<StatefulRedisConnection<String, String>>().coroutines()
    }
}