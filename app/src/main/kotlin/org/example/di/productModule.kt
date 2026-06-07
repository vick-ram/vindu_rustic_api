package org.example.di

import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import org.example.controllers.backend.CategoryController
import org.example.controllers.backend.DiscountController
import org.example.controllers.backend.ProductController
import org.example.controllers.backend.ProductReviewController
import org.example.data.cache.CachedCategoryRepository
import org.example.data.cache.CachedProductRepository
import org.example.data.cache.CachedProductReviewRepository
import org.example.data.mappers.CategoryMapper
import org.example.data.mappers.ProductMapper
import org.example.data.repo.CategoryRepositoryImpl
import org.example.data.repo.CrudCache
import org.example.data.repo.ProductRepositoryImpl
import org.example.data.repo.ProductReviewRepositoryImpl
import org.example.domain.models.catalog.Category
import org.example.domain.models.catalog.Product
import org.example.domain.models.catalog.ProductReview
import org.example.domain.repo.CategoryRepository
import org.example.domain.repo.CrudRepository
import org.example.domain.repo.ProductRepository
import org.example.domain.repo.ProductReviewRepository
import org.example.services.*
import org.koin.core.qualifier.named
import org.koin.dsl.module

@OptIn(ExperimentalLettuceCoroutinesApi::class)
val productModule = module {
    single { CategoryMapper }
    single { ProductMapper }

    single<CrudRepository<Category, String>>(named("categoryReal")) { CategoryRepositoryImpl(get()) }
    single<CategoryRepository>(named("categoryReal")) { get<CrudRepository<Category, String>>(named("categoryReal")) as CategoryRepository }
    single<CrudRepository<Product, String>>(named("productReal")) { ProductRepositoryImpl(get()) }
    single<ProductRepository>(named("productReal")) { get<CrudRepository<Category, String>>(named("productReal")) as ProductRepository }
    single<CrudRepository<ProductReview, String>>(named("reviewReal")) { ProductReviewRepositoryImpl(get()) }
    single<ProductReviewRepository>(named("reviewReal")) { get<CrudRepository<Category, String>>(named("reviewReal")) as ProductReviewRepository }

    single<CrudRepository<Category, String>>(named("categoryCache")) {
        CrudCache(
            redis = get(),
            delegate = get(named("categoryReal")),
            clazz = Category::class.java,
            getId = { it.id },
            cacheName = "category-cache"
        )
    }
    single<CrudRepository<Product, String>>(named("productCache")) {
        CrudCache(
            redis = get(),
            delegate = get(named("productReal")),
            clazz = Product::class.java,
            getId = { it.id },
            cacheName = "product-cache"
        )
    }
    single<CrudRepository<ProductReview, String>>(named("reviewCache")) {
        CrudCache(
            redis = get(),
            delegate = get(named("reviewReal")),
            clazz = ProductReview::class.java,
            getId = { it.id },
            cacheName = "product-review-cache"
        )
    }

    single<CategoryRepository> {
        CachedCategoryRepository(
            delegate = get(named("categoryReal")),
            cache = get(named("categoryCache"))
        )
    }
    single<ProductRepository> {
        CachedProductRepository(
            delegate = get(named("productReal")),
            cache = get(named("productCache"))
        )
    }
    single<ProductReviewRepository> {
        CachedProductReviewRepository(
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