package org.example.controllers.frontend

import io.ktor.http.content.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.example.domain.models.*
import org.example.domain.models.catalog.Category
import org.example.domain.models.catalog.CreateProductRequest
import org.example.domain.models.catalog.Discount
import org.example.domain.models.catalog.Product
import org.example.domain.models.catalog.ProductReview
import org.example.domain.models.catalog.SpecialOffer
import org.example.domain.models.identity.User
import org.example.domain.models.sales.Order
import org.example.services.*
import org.example.utils.respondHtml

class AdminController(
    private val userService: UserService,
    private val roleService: RoleService,
    private val orderService: OrderService,
    private val reviewService: ProductReviewService,
    private val categoryService: CategoryService,
    private val productService: ProductService,
    private val discountService: DiscountService,
    private val specialOfferService: SpecialOfferService
) {
    fun Route.adminRoutes() {
        route("/admin/") {
//            requireAuth("admin", userService, roleService) {
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
                call.respondHtml<Unit>(
                    template = "admin/pages/dashboard",
                    message = "",
                    mapData = mutableMapOf(
                        "currentPage" to "dashboard",
                        "activeTab" to "tab1",
                        "tabs" to tabs
                    )
                )
            }

            get("users") {
                val users = userService.getUsers(queryParams = emptyMap())
                val activeUsersCount = users.count { it.active }
                val inactiveUsersCount = users.count { !it.active }

                call.respondHtml(
                    template = "admin/pages/users/index",
                    data = users,
                    mapData = mutableMapOf(
                        "currentPage" to "users",
                        "columns" to User.columns,
                        "rows" to User.toRows(users),
                        "selectable" to true,
                        "activeUsersCount" to activeUsersCount,
                        "inactiveUsersCount" to inactiveUsersCount,
                        "totalUsersCount" to users.size
                    )
                )
            }

            get("users/detail/{id}") {
                call.respondHtml<Unit>(template = "admin/pages/users/detail")
            }

            route("products/") {
                route("categories") {
                    get {
                        val categories = categoryService.getCategories(0, 100, emptyMap())
                        call.respondHtml(
                            template = "admin/pages/products/categories", data = categories, mapData = mutableMapOf(
                                "currentPage" to "categories",
                                "selectable" to false,
                                "columns" to Category.columns,
                                "rows" to Category.toRow(categories)
                            )
                        )
                    }
                    post {
                        val multipart = call.receiveMultipart()
                        categoryService.createCategory(Category.multipartFormData(multipart))
                    }
                }

                get("new") {
                    call.respondHtml<Unit>(template = "admin/pages/products/new")
                }

                post {
                    val productMultipart = call.receiveMultipart()
                    val mediaFiles: MutableList<PartData.FileItem> = mutableListOf()
                    val requestData = CreateProductRequest.formMultipart(productMultipart, mediaFiles).validate()
                    productService.createProduct(requestData, mediaFiles)

                    call.respondRedirect("/admin/products/list")
                }

                get("list") {
                    val products = productService.getProducts(0, 100, emptyMap())
                    call.respondHtml(
                        template = "admin/pages/products/list", data = products, mapData = mutableMapOf(
                            "currentPage" to "products",
                            "selectable" to true,
                            "columns" to Product.columns,
                            "rows" to Product.toRows(products)
                        )
                    )
                }

                route("discount") {
                    get {
                        call.respondHtml<Unit>(template = "admin/pages/products/discount")
                    }

                    post {
                        val params = call.receiveParameters()
                        val formData = Discount.formParameters(params)
                        discountService.createDiscount(formData)
                    }
                }

                get("reviews") {
                    val reviews = reviewService.getAllReviews(0, 100, emptyMap())
                    call.respondHtml(template = "admin/pages/products/reviews", data = reviews, mapData = mutableMapOf(
                        "currentPage" to "reviews",
                        "selectable" to true,
                        "columns" to ProductReview.columns,
                        "rows" to ProductReview.toRows(reviews)
                    ))
                }

                route("offer") {
                    post {
                        val params = call.receiveParameters()
                        val requestData = SpecialOffer.formParameters(params).validate()
                        specialOfferService.createSpecialOffer(requestData)
                    }
                }
            }
            get("orders") {
                val orders = orderService.getOrders(10, 100, emptyMap())
                call.respondHtml(template = "admin/pages/orders/index", data = orders, mapData = mutableMapOf(
                    "currentPage" to "analytics",
                    "columns" to Order.columns,
                    "rows" to Order.toRows(orders),
                    "selectable" to true,
                    "emptyText" to "No orders found"
                ))
            }

            get("analytics") {
                call.respondHtml<Unit>(template = "admin/pages/analytics", mapData = mutableMapOf(
                    "currentPage" to "analytics"
                ))
            }

            get("settings") {
                call.respondHtml<Unit>(template = "admin/pages/settings", mapData = mutableMapOf(
                    "currentPage" to "settings"
                ))
            }

            get("help") {
                call.respondHtml<Unit>(template = "admin/pages/help", mapData = mutableMapOf(
                    "currentPage" to "help"
                ))
            }

            get("profile") {
                call.respondHtml<Unit>(template = "admin/pages/profile", mapData = mutableMapOf("currentPage" to "profile"))
            }
//            }
        }
    }
}