package org.example.controllers.frontend.ecommerce

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receiveParameters
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.sessions.clear
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions
import org.example.di.Component
import org.example.di.Inject
import org.example.plugins.AuthSession
import org.example.routes.requireAuth
import org.example.services.CartItemService
import org.example.services.CategoryService
import org.example.services.ProductService
import org.example.services.ShoppingCartService
import org.example.services.UserService
import org.example.utils.respondHtml

@Component
class EcommerceController @Inject constructor(
    private val userService: UserService,
    private val productService: ProductService,
    private val categoryService: CategoryService,
    private val shoppingCartService: ShoppingCartService,
    private val cartItemService: CartItemService
) {
    fun Route.ecommerceRoutes() {
        route("/") {
            get {
                val user = call.sessions.get<AuthSession>()?.let { userService.getUser(it.userId) }
                val featuredProducts = productService.getFeaturedProducts()
                val latestProducts = productService.getLatestProducts()
                call.respondHtml(
                    template = "ecommerce/pages/index",
                    data = featuredProducts,
                    mapData = mutableMapOf(
                        "currentPage" to "home",
                        "isLoggedIn" to (user != null),
//                        "user" to user!!,
                        "featuredProducts" to featuredProducts,
                        "latestProducts" to latestProducts,
                        "categories" to categoryService.getActiveCategories()
                    )
                )
            }

            get("products") {
                val query = call.queryParameters["q"].orEmpty()
                val categoryId = call.queryParameters["categoryId"]
                val products = when {
                    query.isNotBlank() -> productService.searchProducts(query)
                    categoryId != null -> productService.getProductsByCategory(categoryId)
                    else -> productService.getProductsByStatus("published")
                }

                call.respondHtml(
                    template = "ecommerce/pages/index",
                    data = products,
                    mapData = mutableMapOf(
                        "currentPage" to "products",
                        "products" to products,
                        "categories" to categoryService.getActiveCategories(),
                        "searchQuery" to query
                    )
                )
            }

            route("cart") {
                get {
                    val userId = call.sessions.get<AuthSession>()?.userId
                        ?: return@get call.respondHtml<Unit>(
                            template = "ecommerce/pages/cart",
                            status = HttpStatusCode.Unauthorized,
                            errors = listOf("Sign in to view your cart")
                        )
                    call.respondCart(userId)
                }

                post("items") {
                    val userId = call.sessions.get<AuthSession>()?.userId
                        ?: return@post call.respondHtml<Unit>(
                            template = "ecommerce/pages/cart",
                            status = HttpStatusCode.Unauthorized,
                            errors = listOf("Sign in to update your cart")
                        )
                    val parameters = call.receiveParameters()
                    val variantId = parameters["variantId"]
                        ?: return@post call.respondHtml<Unit>(
                            template = "ecommerce/pages/cart",
                            status = HttpStatusCode.BadRequest,
                            errors = listOf("variantId is required")
                        )
                    val quantity = parameters["quantity"]?.toIntOrNull() ?: 1
                    if (quantity < 1) {
                        return@post call.respondHtml<Unit>(
                            template = "ecommerce/pages/cart",
                            status = HttpStatusCode.BadRequest,
                            errors = listOf("quantity must be at least 1")
                        )
                    }

                    val cart = shoppingCartService.getUserCart(userId)
                    cartItemService.addOrUpdateItem(cart.id, variantId, quantity)
                    call.respondCart(userId, "Cart updated")
                }

                post("items/remove") {
                    val userId = call.sessions.get<AuthSession>()?.userId
                        ?: return@post call.respondHtml<Unit>(
                            template = "ecommerce/pages/cart",
                            status = HttpStatusCode.Unauthorized,
                            errors = listOf("Sign in to update your cart")
                        )
                    val cartItemId = call.receiveParameters()["cartItemId"]
                        ?: return@post call.respondHtml<Unit>(
                            template = "ecommerce/pages/cart",
                            status = HttpStatusCode.BadRequest,
                            errors = listOf("cartItemId is required")
                        )
                    cartItemService.deleteItem(cartItemId)
                    call.respondCart(userId, "Item removed from cart")
                }
            }

            get("checkout") {
                //                val user = call.sessions.get<AuthSession>()?.let { userService.getUser(it.userId) }
//                    ?: return@get call.respondHtml<Unit>(
//                        template = "ecommerce/pages/checkout",
//                        status = HttpStatusCode.Unauthorized,
//                        errors = listOf("Sign in before checking out")
//                    )
                data class CheckoutStep(
                    val title: String,
                    val subtitle: String? = null,
                    val contentFragment: String
                )

                val checkoutSteps = listOf(
                    CheckoutStep(
                        title = "Cart",
                        subtitle = "Your cart information",
                        contentFragment = "ecommerce/pages/checkout/cart"
                    ),
                    CheckoutStep(
                        title = "Address",
                        subtitle = "Your shipping address",
                        contentFragment = "ecommerce/pages/checkout/address"
                    ),
                    CheckoutStep(
                        title = "Payment",
                        subtitle = "Choose payment method",
                        contentFragment = "ecommerce/pages/checkout/payment"
                    ),
                    CheckoutStep(
                        title = "Review",
                        subtitle = "Confirm order",
                        contentFragment = "ecommerce/pages/checkout/review"
                    )
                )

                val currentStep = call.request.queryParameters["step"]
                    ?.toIntOrNull()
                    ?.coerceIn(0, checkoutSteps.lastIndex)
                    ?: 0

                call.respondHtml<Unit>(
                    template = "ecommerce/pages/checkout/index",
                    mapData = mutableMapOf(
                        "currentPage" to "checkout",
                        "currentStep" to currentStep,
                        "steps" to checkoutSteps,
//                        "user" to user,
                        "session" to emptyMap<String, Any>()
                    )
                )
            }
        }
    }

    private suspend fun ApplicationCall.respondCart(userId: String, message: String? = null) {
        val cart = shoppingCartService.getUserCart(userId)
        val items = cartItemService.getItems(cart.id)
        val total = cartItemService.getCartTotal(cart.id)

        respondHtml(
            template = "ecommerce/pages/cart",
            data = items,
            message = message,
            mapData = mutableMapOf(
                "currentPage" to "cart",
                "cart" to cart,
                "items" to items,
                "itemCount" to (total?.itemCount ?: 0),
                "totalQuantity" to (total?.totalQuantity ?: 0)
            )
        )
    }
}
