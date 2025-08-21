package org.example.di

import org.example.controllers.UserController
import org.example.data.db.entities.UserEntity
import org.example.data.mappers.UserMapper
import org.example.data.repo.UserRepositoryImpl
import org.example.domain.models.User
import org.example.domain.repo.CrudRepository
import org.example.domain.repo.UserRepository
import org.example.utils.createCrudCache
import org.koin.dsl.module

val userModule = module {
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