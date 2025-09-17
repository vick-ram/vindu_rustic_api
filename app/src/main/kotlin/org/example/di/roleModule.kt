package org.example.di

import org.example.controllers.backend.RoleController
import org.example.data.db.entities.RoleEntity
import org.example.data.mappers.RoleMapper
import org.example.data.repo.RoleRepositoryImpl
import org.example.domain.models.Role
import org.example.domain.repo.CrudRepository
import org.example.domain.repo.RoleRepository
import org.example.services.RoleService
import org.example.utils.createCrudCache
import org.koin.dsl.module

val roleModule  = module {
    // Role Injection
    single { RoleMapper }
    single<CrudRepository<Role, String>> {
        createCrudCache(
            entityClass = RoleEntity,
            getId = { it.id },
            toDomain = RoleMapper::toModel,
            toEntity = RoleMapper::toEntity,
            cacheName = "role-cache"
        )
    }
    single<RoleRepository> { RoleRepositoryImpl(get()) }
    single { RoleService(get()) }
    single { RoleController(get<RoleService>()) }
}