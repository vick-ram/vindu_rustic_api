package org.example.controllers

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import org.example.domain.models.CartRequest
import org.example.domain.models.RemoveFromCart
import org.example.domain.models.UpdateCartQuantity
import org.example.services.CartService
import org.example.utils.respondApi

class CartController(
    private val cartService: CartService
) {
    fun Route.cartRoutes() {

        route("/cart/") {
            authenticate("auth-jwt") {
                get {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.subject
                    val cart = cartService.getCart(userId!!)
                    call.respondApi(
                        data = cart,
                        message = "User cart"
                    )
                }

                authenticate("auth-jwt") {
                    post("add") {
                        val principal = call.principal<JWTPrincipal>()
                        val userId = principal?.subject ?: ""
                        val cartRequest = call.receive<CartRequest>().validate()
                        val newCart = cartService.addToCart(userId, cartRequest.productId, cartRequest.quantity)
                        call.respondApi(
                            status = HttpStatusCode.Created,
                            data = newCart,
                            message = "Product added to cart"
                        )
                    }
                }

                authenticate("auth-jwt") {
                    post("remove") {
                        val principal = call.principal<JWTPrincipal>()
                        val userId = principal?.subject ?: ""
                        val cartRequest = call.receive<RemoveFromCart>().validate()
                        val res = cartService.removeFromCart(userId, cartRequest.productId)
                        call.respondApi(
                            status = HttpStatusCode.Accepted,
                            data = res,
                            message = "Product removed from cart"
                        )
                    }
                }

                authenticate("auth-jwt") {
                    put("update") {
                        val principal = call.principal<JWTPrincipal>()
                        val userId = principal?.subject ?: ""
                        val cartRequest = call.receive<UpdateCartQuantity>()

                        val res = cartService.updateCartItem(userId, cartRequest.productId, cartRequest.quantity)
                        call.respondApi(
                            status = HttpStatusCode.Accepted,
                            data = res,
                            message = "Product updated in cart"
                        )
                    }
                }

                authenticate("auth-jwt") {
                    post("clear") {
                        val principal = call.principal<JWTPrincipal>()
                        val userId = principal?.subject ?: ""
                        cartService.clearCart(userId)
                        call.respondApi<Unit>(
                            status = HttpStatusCode.NoContent,
                            message = "Cart items cleared"
                        )
                    }
                }
            }
        }
    }
}