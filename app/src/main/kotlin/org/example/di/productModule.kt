package org.example.di

import org.example.data.mappers.CategoryMapper
import org.example.data.mappers.ProductMapper
import org.example.data.repo.CategoryRepositoryImpl
import org.example.data.repo.ProductRepositoryImpl
import org.example.domain.repo.CategoryRepository
import org.example.domain.repo.ProductRepository
import org.koin.dsl.module

val productModule = module {
    single<ProductRepository> { ProductRepositoryImpl(get<ProductMapper>()) }
    single<CategoryRepository> { CategoryRepositoryImpl(get<CategoryMapper>()) }
}