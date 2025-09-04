package org.example.di

import org.example.controllers.CategoryController
import org.example.controllers.ProductController
import org.example.controllers.ProductReviewController
import org.example.data.db.entities.CategoryEntity
import org.example.data.db.entities.ProductEntity
import org.example.data.db.entities.ProductReviewEntity
import org.example.data.mappers.CategoryMapper
import org.example.data.mappers.MediaMapper
import org.example.data.mappers.ProductMapper
import org.example.data.mappers.ProductReviewMapper
import org.example.data.repo.CategoryRepositoryImpl
import org.example.data.repo.ProductRepositoryImpl
import org.example.data.repo.ProductReviewRepositoryImpl
import org.example.domain.models.Category
import org.example.domain.models.Product
import org.example.domain.models.ProductReview
import org.example.domain.repo.CategoryRepository
import org.example.domain.repo.CrudRepository
import org.example.domain.repo.ProductRepository
import org.example.domain.repo.ProductReviewRepository
import org.example.services.CategoryService
import org.example.services.ProductReviewService
import org.example.services.ProductService
import org.example.utils.createCrudCache
import org.koin.dsl.module

val productModule = module {
    single { CategoryMapper }

    single<CrudRepository<Category, String>> {
        createCrudCache(
            entityClass = CategoryEntity,
            getId = { it.id },
            toDomain = CategoryMapper::toModel,
            toEntity = CategoryMapper::toEntity,
            cacheName = "category-cache"
        )
    }
    single<CategoryRepository> { CategoryRepositoryImpl(get()) }
    single { CategoryService(get()) }
    single { CategoryController(get<CategoryService>()) }


    single { ProductMapper }
    single { MediaMapper }
    single<CrudRepository<Product, String>> {
        createCrudCache(
            entityClass = ProductEntity,
            getId = { it.id },
            toDomain = ProductMapper::toModel,
            toEntity = ProductMapper::toEntity,
            cacheName = "product-cache"
        )
    }
    single<ProductRepository> { ProductRepositoryImpl(get()) }
    single { ProductService(get(), get()) }
    single { ProductController(get<ProductService>()) }

    // Review
    single { ProductReviewMapper }
    single<CrudRepository<ProductReview, String>> {
        createCrudCache(
            entityClass = ProductReviewEntity,
            getId = { it.id },
            toDomain = ProductReviewMapper::toModel,
            toEntity = ProductReviewMapper::toEntity,
            cacheName = "product-review-cache"
        )
    }
    single<ProductReviewRepository> { ProductReviewRepositoryImpl(get()) }
    single { ProductReviewService(get()) }
    single { ProductReviewController(get<ProductReviewService>()) }

}