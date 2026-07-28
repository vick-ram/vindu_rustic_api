package org.example.controllers.backend

import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import org.example.domain.models.sales.CartRequest
import org.example.domain.models.sales.RemoveFromCart
import org.example.domain.models.sales.UpdateCartQuantity
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
                    val cart = cartService.getCart(userId!!, "")
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
                        val newCart = cartService.addToCart(userId = userId, sessionId = "",cartRequest.productId, cartRequest.quantity)
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
                        val res = cartService.removeFromCart(userId, "",cartRequest.productId)
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

                        val res = cartService.updateCartItem(userId, "",cartRequest.productId, cartRequest.quantity)
                        call.respondApi(
                            status = HttpStatusCode.Accepted,
                            data = res,
                            message = "Product updated in cart"
                        )
                    }
                }

//                authenticate("auth-jwt") {
//                    post("clear") {
//                        val principal = call.principal<JWTPrincipal>()
//                        val userId = principal?.subject ?: ""
//                        cartService.clearCart(userId)
//                        call.respondApi<Unit>(
//                            status = HttpStatusCode.NoContent,
//                            message = "Cart items cleared"
//                        )
//                    }
//                }
            }
        }
    }
}