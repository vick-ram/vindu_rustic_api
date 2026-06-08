package org.example.di

import org.example.controllers.backend.CartController
import org.example.data.mappers.CartItemMapper
import org.example.data.mappers.CartMapper
import org.example.data.repo.CartRepositoryImpl
import org.example.domain.models.sales.ShoppingCart
import org.example.domain.repo.CrudRepository
import org.example.domain.repo.ShoppingCartRepository
import org.example.services.CartService
import org.example.services.ShoppingCartService
import org.koin.core.qualifier.named
import org.koin.dsl.module

val cartModule = module {
    single { CartMapper }
    single { CartItemMapper }

    single<CrudRepository<ShoppingCart, String>>(named("cartReal")) { CartRepositoryImpl(get()) }
    single<ShoppingCartRepository>(named("cartReal")) { get<CrudRepository<ShoppingCart, String>>(named("cartReal")) as ShoppingCartRepository }

    single { ShoppingCartService(get(named("cartReal"))) }
    single { CartController(get()) }
}