package org.example.di

import org.example.controllers.OrderController
import org.example.data.mappers.OrderMapper
import org.example.data.repo.OrderRepositoryImpl
import org.example.domain.repo.OrderRepository
import org.example.services.OrderService
import org.koin.dsl.module

val orderModule = module {
    single<OrderRepository> { OrderRepositoryImpl(get<OrderMapper>()) }
    single { OrderService(get<OrderRepository>()) }
    single { OrderController(get<OrderService>()) }
}