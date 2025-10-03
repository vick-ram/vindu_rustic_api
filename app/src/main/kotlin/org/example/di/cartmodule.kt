package org.example.di

import org.example.controllers.backend.CartController
import org.example.data.mappers.CartItemMapper
import org.example.data.mappers.CartMapper
import org.example.data.repo.CartRepositoryImpl
import org.example.domain.models.Cart
import org.example.domain.repo.CartRepository
import org.example.domain.repo.CrudRepository
import org.example.services.CartService
import org.koin.core.qualifier.named
import org.koin.dsl.module

val cartModule = module {
    single { CartMapper }
    single { CartItemMapper }

    single<CrudRepository<Cart, String>>(named("cartReal")) { CartRepositoryImpl(get()) }
    single<CartRepository>(named("cartReal")) { get<CrudRepository<Cart, String>>(named("cartReal")) as CartRepository }

    single { CartService(get(named("cartReal")), get()) }
    single { CartController(get()) }
}