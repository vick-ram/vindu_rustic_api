package org.example.di

import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import org.example.controllers.backend.UserController
import org.example.data.mappers.DeviceTokenMapper
import org.example.data.mappers.UserMapper
import org.example.data.repo.CrudCache
import org.example.data.repo.DeviceTokenRepositoryImpl
import org.example.data.repo.UserRepositoryImpl
import org.example.domain.models.system.DeviceToken
import org.example.domain.models.identity.User
import org.example.domain.repo.CrudRepository
import org.example.domain.repo.DeviceTokenRepository
import org.example.domain.repo.UserRepository
import org.example.services.DeviceTokenService
import org.example.services.UserService
import org.example.config.DynamicRouteFactory
import org.example.data.cache.CachedUserRepository
import org.example.services.TokenService
import org.koin.core.qualifier.named
import org.koin.dsl.module

@OptIn(ExperimentalLettuceCoroutinesApi::class)
val userModule = module {
    single { UserMapper }
    single { DeviceTokenMapper }
    single { DynamicRouteFactory() }

    // Register real repo under both CrudRepository and UserRepository
    single<CrudRepository<User, String>>(named("userReal")) { UserRepositoryImpl(get(), get()) }
    single<UserRepository>(named("userReal")) { get<CrudRepository<User, String>>(named("userReal")) as UserRepository }

    single<CrudRepository<DeviceToken, String>>(named("deviceTokenReal")) { DeviceTokenRepositoryImpl(get()) }
    single<DeviceTokenRepository>(named("deviceTokenReal")) { get<CrudRepository<DeviceToken, String>>(named("deviceTokenReal")) as DeviceTokenRepository }


    // Cache wrapping the real repo
    single<CrudRepository<User, String>>(named("userCache")) {
        CrudCache(
            redis = get(),
            delegate = get(named("userReal")), // resolves CrudRepository<User, String>
            clazz = User::class.java,
            getId = { it.id },
            cacheName = "user-cache"
        )
    }

    // Cached repo (default binding for UserRepository)
    single<UserRepository> {
        CachedUserRepository(
            delegate = get(named("userReal")), // resolves UserRepository
            cache = get(named("userCache"))    // resolves CrudRepository<User, String>
        )
    }

    single { TokenService(get(), get()) }
    single { UserService(get(), get()) }
    single { UserController(get()) }
    single { DeviceTokenService(get()) }
}
