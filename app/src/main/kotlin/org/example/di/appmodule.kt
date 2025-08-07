package org.example.di

import io.ktor.server.application.Application
import io.ktor.server.application.install
import org.example.controllers.UserController
import org.example.data.db.entities.UserEntity
import org.example.data.mappers.UserMapper
import org.example.data.repo.UserRepositoryImpl
import org.example.domain.models.User
import org.example.domain.repo.CrudRepository
import org.example.domain.repo.UserRepository
import org.example.utils.createCrudCache
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

val appModule = module {
    single<CrudRepository<User, String>> {
        createCrudCache(
            entityClass = UserEntity,
            getId = { it.id },
            toDomain = UserMapper::toModel,
            toEntity = UserMapper::toEntity
        )
    }
    single<UserRepository> { UserRepositoryImpl() }
    single { UserController(get<UserRepository>()) }
}

fun Application.configureDI() {
    install(Koin) {
        slf4jLogger()
        modules(appModule)
    }
}
