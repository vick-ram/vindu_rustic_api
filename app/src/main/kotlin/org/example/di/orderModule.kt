package org.example.di

import org.example.controllers.backend.OrderController
import org.example.data.mappers.OrderItemMapper
import org.example.data.mappers.OrderMapper
import org.example.data.repo.CrudCache
import org.example.data.repo.OrderRepositoryImpl
import org.example.domain.models.sales.Order
import org.example.domain.repo.CachedOrder
import org.example.domain.repo.CrudRepository
import org.example.domain.repo.OrderRepository
import org.example.services.OrderService
import org.koin.core.qualifier.named
import org.koin.dsl.module

val orderModule = module {
    single { OrderMapper }
    single { OrderItemMapper }

    single<CrudRepository<Order, String>>(named("orderReal")) { OrderRepositoryImpl(get()) }
    single<OrderRepository>(named("orderReal")) { get<CrudRepository<Order, String>>(named("orderReal")) as OrderRepository }

    // Cache wrapping the real repo
    single<CrudRepository<Order, String>>(named("orderCache")) {
        CrudCache(
            delegate = get(named("orderReal")),
            clazz = Order::class.java,
            getId = { it.id },
            cacheName = "order-cache"
        )
    }

    single<OrderRepository> {
        CachedOrder(
            delegate = get(named("orderReal")),
            cache = get(named("orderCache"))
        )
    }

    single { OrderService(get()) }
    single { OrderController(get()) }
}