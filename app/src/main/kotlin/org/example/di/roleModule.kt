package org.example.di

import org.example.controllers.backend.RoleController
import org.example.data.mappers.RoleMapper
import org.example.data.repo.CrudCache
import org.example.data.repo.RoleRepositoryImpl
import org.example.domain.models.Role
import org.example.domain.repo.CachedRole
import org.example.domain.repo.CrudRepository
import org.example.domain.repo.RoleRepository
import org.example.services.RoleService
import org.koin.core.qualifier.named
import org.koin.dsl.module

val roleModule = module {
    single { RoleMapper }

    // Register real repo under both CrudRepository and UserRepository
    single<CrudRepository<Role, String>>(named("roleReal")) { RoleRepositoryImpl(get()) }
    single<RoleRepository>(named("roleReal")) { get<CrudRepository<Role, String>>(named("roleReal")) as RoleRepository }

    // Cache wrapping the real repo
    single<CrudRepository<Role, String>>(named("realCache")) {
        CrudCache(
            delegate = get(named("roleReal")), // resolves CrudRepository<User, String>
            clazz = Role::class.java,
            getId = { it.id },
            cacheName = "user-cache"
        )
    }

    // Cached repo (default binding for UserRepository)
    single<RoleRepository> {
        CachedRole(
            delegate = get(named("roleReal")), // resolves UserRepository
            cache = get(named("realCache"))    // resolves CrudRepository<User, String>
        )
    }

    single { RoleService(get()) }
    single { RoleController(get()) }
}
