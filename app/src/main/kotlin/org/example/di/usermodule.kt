package org.example.di

import org.example.controllers.backend.UserController
import org.example.data.db.entities.UserEntity
import org.example.data.mappers.UserMapper
import org.example.data.repo.CrudCache
import org.example.data.repo.UserRepositoryImpl
import org.example.domain.models.User
import org.example.domain.repo.CrudRepository
import org.example.domain.repo.UserRepository
import org.example.services.UserService
import org.example.utils.createCrudCache
import org.koin.dsl.module
import java.io.File

//val userModule = module {
//    single { UserMapper }
//    single<CrudRepository<User, String>> {
//        createCrudCache(
//            entityClass = UserEntity,
//            getId = { it.id },
//            toDomain = UserMapper::toModel,
//            toEntity = UserMapper::toEntity,
//            cacheName = "user-cache"
//        )
//    }
//    single<UserRepository> { UserRepositoryImpl(get<UserMapper>()) }
//    single { UserService(get()) }
//    single { UserController(get<UserService>()) }
//}

val userModule = module {
    single { UserMapper }

    // First create the actual repository
    single<UserRepository> { UserRepositoryImpl(get()) }

    // Then wrap it with cache for CrudRepository operations
    single<CrudRepository<User, String>> {
        CrudCache(
            delegate = get<UserRepository>(),  // Use the actual repository as delegate
            clazz = User::class.java,
            idClazz = String::class.java,
            storageFile = File("app/build/cache"),
            getId = { it.id },
            cacheName = "user-cache"
        )
    }

    single { UserService(get()) }
    single { UserController(get()) }
}