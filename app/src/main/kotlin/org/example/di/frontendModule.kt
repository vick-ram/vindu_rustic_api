package org.example.di

import org.example.controllers.frontend.AdminController
import org.example.controllers.frontend.AuthController
import org.example.controllers.frontend.EcommerceController
import org.example.controllers.frontend.RootController
import org.example.services.CartService
import org.example.services.CategoryService
import org.example.services.DiscountService
import org.example.services.OrderService
import org.example.services.ProductReviewService
import org.example.services.ProductService
import org.example.services.RoleService
import org.example.services.SpecialOfferService
import org.example.services.UserService
import org.koin.dsl.module

val frontendModule = module {
    single { RootController(get<UserService>(), get<RoleService>()) }
    single { AuthController(get<UserService>(), get<RoleService>(), get<CartService>()) }
    single {
        AdminController(
            get<UserService>(),
            get<RoleService>(),
            get<OrderService>(),
            get<ProductReviewService>(),
            get<CategoryService>(),
            get<ProductService>(),
            get<DiscountService>(),
            get<SpecialOfferService>()
        )
    }
    single { EcommerceController(get<UserService>(), get<RoleService>(), get<ProductService>(), get<CartService>()) }
}