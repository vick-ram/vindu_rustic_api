package org.example.di

import org.example.controllers.backend.CartController
import org.example.data.db.entities.CartEntity
import org.example.data.db.entities.CartItemEntity
import org.example.data.mappers.CartItemMapper
import org.example.data.mappers.CartMapper
import org.example.data.repo.CartRepositoryImpl
import org.example.services.CartService
import org.example.domain.models.Cart
import org.example.domain.models.CartItem
import org.example.domain.repo.CartRepository
import org.example.domain.repo.CrudRepository
import org.example.utils.createCrudCache
import org.koin.dsl.module

val cartModule = module {
    single { CartMapper }
    single<CrudRepository<Cart, String>> {
        createCrudCache(
            entityClass = CartEntity,
            getId = { it.id },
            toDomain = CartMapper::toModel,
            toEntity = CartMapper::toEntity,
            cacheName = "cart-cache"
        )
    }

    single<CrudRepository<CartItem, String>> {
        createCrudCache(
            entityClass = CartItemEntity,
            getId = { it.id },
            toDomain = CartItemMapper::toModel,
            toEntity = CartItemMapper::toEntity,
            cacheName = "cart-item-cache"
        )
    }
    single<CartRepository> { CartRepositoryImpl(get()) }
    single { CartService(get()) }
    single { CartController(get<CartService>()) }
}