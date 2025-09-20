package org.example.di

import org.example.controllers.backend.CategoryController
import org.example.controllers.backend.ProductController
import org.example.controllers.backend.ProductReviewController
import org.example.data.mappers.CategoryMapper
import org.example.data.mappers.MediaMapper
import org.example.data.mappers.ProductMapper
import org.example.data.mappers.ProductReviewMapper
import org.example.data.repo.CategoryRepositoryImpl
import org.example.data.repo.CrudCache
import org.example.data.repo.ProductRepositoryImpl
import org.example.data.repo.ProductReviewRepositoryImpl
import org.example.domain.models.Category
import org.example.domain.models.Product
import org.example.domain.models.ProductReview
import org.example.domain.repo.*
import org.example.services.CategoryService
import org.example.services.ProductReviewService
import org.example.services.ProductService
import org.koin.core.qualifier.named
import org.koin.dsl.module

val productModule = module {
    single { CategoryMapper }
    single { ProductMapper }
    single { ProductReviewMapper }
    single { MediaMapper }

    single<CrudRepository<Category, String>>(named("categoryReal")) { CategoryRepositoryImpl(get()) }
    single<CategoryRepository>(named("categoryReal")) { get<CrudRepository<Category, String>>(named("categoryReal")) as CategoryRepository }
    single<CrudRepository<Product, String>>(named("productReal")) { ProductRepositoryImpl(get()) }
    single<ProductRepository>(named("productReal")) { get<CrudRepository<Category, String>>(named("productReal")) as ProductRepository }
    single<CrudRepository<ProductReview, String>>(named("reviewReal")) { ProductReviewRepositoryImpl(get()) }
    single<ProductReviewRepository>(named("reviewReal")) { get<CrudRepository<Category, String>>(named("reviewReal")) as ProductReviewRepository }

    single<CrudRepository<Category, String>>(named("categoryCache")) {
        CrudCache(
            delegate = get(named("categoryReal")),
            clazz = Category::class.java,
            getId = { it.id },
            cacheName = "category-cache"
        )
    }
    single<CrudRepository<Product, String>>(named("productCache")) {
        CrudCache(
            delegate = get(named("productReal")),
            clazz = Product::class.java,
            getId = { it.id },
            cacheName = "product-cache"
        )
    }
    single<CrudRepository<ProductReview, String>>(named("reviewCache")) {
        CrudCache(
            delegate = get(named("reviewReal")),
            clazz = ProductReview::class.java,
            getId = { it.id },
            cacheName = "product-review-cache"
        )
    }

    single<CategoryRepository> {
        CachedCategory(
            delegate = get(named("categoryReal")),
            cache = get(named("categoryCache"))
        )
    }
    single<ProductRepository> {
        CachedProduct(
            delegate = get(named("productReal")),
            cache = get(named("productCache"))
        )
    }
    single<ProductReviewRepository> {
        CachedProductReview(
            delegate = get(named("reviewReal")),
            cache = get(named("reviewCache"))
        )
    }

    single { CategoryService(get()) }
    single { CategoryController(get()) }
    single { ProductService(get(), get()) }
    single { ProductController(get()) }
    single { ProductReviewService(get()) }
    single { ProductReviewController(get()) }
}