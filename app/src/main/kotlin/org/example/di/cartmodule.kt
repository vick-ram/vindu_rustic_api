package org.example.di

import org.example.data.db.entities.CartEntity
import org.example.data.db.entities.CartItemEntity
import org.example.data.mappers.CartItemMapper
import org.example.data.mappers.CartItemMapper.toEntity
import org.example.data.mappers.CartItemMapper.toModel
import org.example.data.mappers.CartMapper
import org.example.data.mappers.CartMapper.toEntity
import org.example.data.mappers.CartMapper.toModel
import org.example.data.repo.CartRepositoryImpl
import org.example.services.CartService
import org.example.domain.models.Cart
import org.example.domain.models.CartItem
import org.example.domain.repo.CartRepository
import org.example.domain.repo.CrudRepository
import org.example.utils.createCrudCache
import org.koin.dsl.module

val cartModule = module {
    single<CrudRepository<Cart, String>> {
        createCrudCache(
            entityClass = CartEntity,
            getId = { it.id },
            toDomain = CartMapper::toModel,
            toEntity = CartMapper::toEntity
        )
    }

    single<CrudRepository<CartItem, String>> {
        createCrudCache(
            entityClass = CartItemEntity,
            getId = { it.id },
            toDomain = CartItemMapper::toModel,
            toEntity = CartItemMapper::toEntity
        )
    }
    single<CartRepository> { CartRepositoryImpl(get<CartMapper>()) }
    single { CartService(get<CartRepository>()) }
}