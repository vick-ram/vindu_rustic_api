package org.example.di

import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.RedisClient
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.coroutines
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands
import org.example.config.AppConfig
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
class RedisModule {

    @Single
    fun provideRedisClient(appConfig: AppConfig) : RedisClient {
        val config = appConfig.redis
        return RedisClient.create("redis://${config.host}:${config.port}/${config.database}")
    }

    @Single
    fun provideStatefulConnection(client: RedisClient): StatefulRedisConnection<String, String> {
        return client.connect()
    }

    @OptIn(ExperimentalLettuceCoroutinesApi::class)
    @Single
    fun provideCoroutineCommands(
        connection: StatefulRedisConnection<String, String>
    ): RedisCoroutinesCommands<String, String> {
        return connection.coroutines()
    }
}