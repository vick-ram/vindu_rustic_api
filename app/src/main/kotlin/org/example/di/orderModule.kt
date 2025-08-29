package org.example.di

import org.example.controllers.OrderController
import org.example.data.db.entities.OrderEntity
import org.example.data.db.entities.OrderItemEntity
import org.example.data.mappers.OrderItemMapper
import org.example.data.mappers.OrderMapper
import org.example.data.repo.OrderRepositoryImpl
import org.example.domain.models.Order
import org.example.domain.models.OrderItem
import org.example.domain.repo.CrudRepository
import org.example.domain.repo.OrderRepository
import org.example.services.OrderService
import org.example.utils.createCrudCache
import org.koin.dsl.module

val orderModule = module {
    single { OrderMapper }
    single<CrudRepository<Order, String>> {
        createCrudCache(
            entityClass = OrderEntity,
            getId = { it.id },
            toDomain = OrderMapper::toModel,
            toEntity = OrderMapper::toEntity,
            cacheName = "order-cache"
        )
    }
    single<CrudRepository<OrderItem, String>> {
        createCrudCache(
            entityClass = OrderItemEntity,
            getId = { it.id },
            toDomain = OrderItemMapper::toModel,
            toEntity = OrderItemMapper::toEntity,
            cacheName = "order-item-cache"
        )
    }
    single<OrderRepository> { OrderRepositoryImpl(get()) }
    single { OrderService(get()) }
    single { OrderController(get<OrderService>()) }
}