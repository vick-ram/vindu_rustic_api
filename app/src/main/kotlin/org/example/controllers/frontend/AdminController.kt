package org.example.controllers.frontend

import io.ktor.server.request.receiveMultipart
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.thymeleaf.ThymeleafContent
import org.example.domain.models.Category
import org.example.domain.models.Discount
import org.example.domain.models.Order
import org.example.domain.models.Product
import org.example.domain.models.ProductReview
import org.example.domain.models.User
import org.example.routes.requireAuth
import org.example.services.CategoryService
import org.example.services.DiscountService
import org.example.services.OrderService
import org.example.services.ProductReviewService
import org.example.services.ProductService
import org.example.services.RoleService
import org.example.services.UserService

class AdminController(
    private val userService: UserService,
    private val roleService: RoleService,
    private val orderService: OrderService,
    private val reviewService: ProductReviewService,
    private val categoryService: CategoryService,
    private val productService: ProductService,
    private val discountService: DiscountService
) {
    fun Route.adminRoutes() {
        route("/admin/") {
            requireAuth("admin", userService, roleService) {
                // Redirect
                get {
                    call.respondRedirect("admin/dashboard")
                }

                get("dashboard") {
                    val tabs = listOf(
                        mapOf("id" to "tab1", "label" to "Dashboard", "content" to "<p>Dashboard content here</p>"),
                        mapOf("id" to "tab2", "label" to "Analytics", "content" to "<p>Analytics content here</p>"),
                        mapOf("id" to "tab3", "label" to "Settings", "content" to "<p>Settings content here</p>")
                    )
                    call.respond(
                        ThymeleafContent(
                            "admin/pages/dashboard", mapOf(
                                "currentPage" to "dashboard",
                                "activeTab" to "tab1",
                                "tabs" to tabs
                            )
                        )
                    )
                }

                get("users") {
                    val users = userService.getUsers(queryParams = emptyMap())
                    val activeUsersCount = users.count { it.active }
                    val inactiveUsersCount = users.count { !it.active }

                    call.respond(
                        ThymeleafContent(
                            "admin/pages/users/index", mapOf(
                                "currentPage" to "users",
                                "users" to users,
                                "columns" to User.columns,
                                "rows" to User.toRows(users),
                                "selectable" to true,
                                "activeUsersCount" to activeUsersCount,
                                "inactiveUsersCount" to inactiveUsersCount,
                                "totalUsersCount" to users.size
                            )
                        )
                    )
                }

                get("users/detail/{id}") {
                    call.respond(
                        ThymeleafContent("admin/pages/users/detail", mapOf())
                    )
                }

                route("products/") {
                    route("categories") {
                        get {
                            val categories = categoryService.getCategories(0, 100, emptyMap())
                            call.respond(
                                ThymeleafContent(
                                    "admin/pages/products/categories", mapOf(
                                        "currentPage" to "categories",
                                        "selectable" to false,
                                        "columns" to Category.columns,
                                        "rows" to Category.toRow(categories)
                                    )
                                )
                            )
                        }
                        post {
                            val multipart = call.receiveMultipart()
                            categoryService.createCategory(Category.multipartFormData(multipart))
                        }
                    }

                    get("new") {
                        call.respond(
                            ThymeleafContent(
                                "admin/pages/products/new", mapOf()
                            )
                        )
                    }

                    get("list") {
                        call.respond(
                            ThymeleafContent(
                                "admin/pages/products/list", mapOf(
                                    "currentPage" to "products",
                                    "selectable" to true,
                                    "columns" to Product.columns,
                                    "rows" to Product.toRows(productService.getProducts(0, 100, emptyMap()))
                                )
                            )
                        )
                    }

                    route("discount") {
                        get {
                            call.respond(
                                ThymeleafContent(
                                    "admin/pages/products/discount",
                                    mapOf()
                                )
                            )
                        }

                        post {
                            val params = call.receiveParameters()
                            val formData = Discount.formParameters(params)
                            discountService.createDiscount(formData)
                        }
                    }

                    get("reviews") {
                        val reviews = reviewService.getAllReviews(0, 100, emptyMap())
                        call.respond(
                            ThymeleafContent(
                                "admin/pages/products/reviews", mapOf(
                                    "currentPage" to "reviews",
                                    "selectable" to true,
                                    "columns" to ProductReview.columns,
                                    "rows" to ProductReview.toRows(reviews)
                                )
                            )
                        )
                    }
                }
                get("orders") {
                    val orders = orderService.getOrders(10, 100, emptyMap())
                    call.respond(
                        ThymeleafContent(
                            "admin/pages/orders/index", mapOf(
                                "currentPage" to "orders",
                                "columns" to Order.columns,
                                "rows" to Order.toRows(orders),
                                "selectable" to true,
                                "emptyText" to "No orders found"
                            )
                        )
                    )
                }

                get("analytics") {
                    call.respond(
                        ThymeleafContent(
                            "admin/pages/analytics", mapOf(
                                "currentPage" to "analytics"
                            )
                        )
                    )
                }

                get("settings") {
                    call.respond(
                        ThymeleafContent(
                            "admin/pages/settings", mapOf(
                                "currentPage" to "settings"
                            )
                        )
                    )
                }

                get("help") {
                    call.respond(
                        ThymeleafContent(
                            "admin/pages/help", mapOf(
                                "currentPage" to "help"
                            )
                        )
                    )
                }

                get("profile") {
                    call.respond(
                        ThymeleafContent(
                            "admin/pages/profile", mapOf(
                                "currentPage" to "profile"
                            )
                        )
                    )
                }
            }
        }
    }
}