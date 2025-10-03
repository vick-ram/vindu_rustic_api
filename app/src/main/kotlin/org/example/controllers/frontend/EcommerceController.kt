package org.example.controllers.frontend

import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import io.ktor.server.thymeleaf.*
import org.example.plugins.AuthSession
import org.example.plugins.CartSession
import org.example.services.CartService
import org.example.services.ProductService
import org.example.services.RoleService
import org.example.services.UserService

class EcommerceController(
    private val userService: UserService,
    private val roleService: RoleService,
    private val productService: ProductService,
    private val cartService: CartService
) {
    fun Route.ecommerceRoutes() {
        route("/ecommerce/") {
            get {
                val session = call.sessions.get<AuthSession>()
                val user = session?.let { userService.getUser(it.userId) }
                val model: MutableMap<String, Any> = mutableMapOf()

                model["currentPage"] = "home"
                model["isLoggedIn"] = user != null
                if (user != null) {
                    model["user"] = user
                }

                call.respond(
                    ThymeleafContent(
                        "ecommerce/pages/index", model
                    )
                )
            }

            route("cart/") {
                get {
                    val authSession = call.sessions.get<AuthSession>()
                    val cartSession = call.sessions.get<CartSession>()
                    val cart = cartService.getCart(authSession?.userId, cartSession?.sessionId.toString())

                    call.sessions.set(cart)
                    //Show the car for authenticated
                    call.respond(ThymeleafContent("ecommerce/pages/cart", mapOf(
                        "items" to cart.items,
                        "itemCount" to cart.items.count(),
                        "total" to cart.total.toPlainString(),
                    )))
                }

                post("add") {
                    val productId = call.request.queryParameters["productId"].toString()
                    val quantity = call.request.queryParameters["quantity"]?.toInt() ?: 1

                    val authSession = call.sessions.get<AuthSession>()
                    val cartSession = call.sessions.get<CartSession>() ?: CartSession()
                    val updatedCart = cartService.addToCart(
                        userId = authSession?.userId,
                        sessionId = cartSession.sessionId,
                        productId = productId,
                        quantity = quantity
                    )

                    call.sessions.set(updatedCart)
                    call.respondRedirect("/cart")

                }

                post("remove") {
                    val authSession = call.sessions.get<AuthSession>()
                    val cartSession = call.sessions.get<CartSession>() ?: CartSession()

                    val productId = call.queryParameters["productId"].toString()

                    val updatedCart = cartService.removeFromCart(
                        userId = authSession?.userId,
                        sessionId = cartSession.sessionId,
                        productId = productId
                    )

                    call.sessions.set(updatedCart)
                    call.respondRedirect("/cart")
                }

                post("update") {
                    val authSession = call.sessions.get<AuthSession>()
                    val cartSession = call.sessions.get<CartSession>() ?: CartSession()

                    val productId = call.queryParameters["productId"].toString()
                    val quantity = call.queryParameters["quantity"]?.toInt() ?: 1

                    val updatedCart = cartService.updateCartItem(
                        userId = authSession?.userId,
                        sessionId = cartSession.sessionId,
                        productId = productId,
                        quantity = quantity
                    )

                    call.sessions.set(updatedCart)
                    call.respondRedirect("/cart")
                }
            }

            get("checkout") {
                val session = call.sessions.get<AuthSession>()
                if (session == null) {
                    call.respondRedirect("/signin?redirect=/ecommerce/pages/cart")
                    return@get
                }

                val user = userService.getUser(session.userId)
                val role = user?.roleId?.let { roleService.getRole(it) }?.name

                if (role != "customer") {
                    call.respondRedirect("/ecommerce?error=access_denied")
                    return@get
                }
                call.respond(ThymeleafContent("ecommerce/pages/checkout", mapOf()))
            }
        }
    }
}