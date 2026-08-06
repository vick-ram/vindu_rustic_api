package org.example.controllers.frontend.admin

import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import org.example.di.Component
import org.example.di.Inject
import org.example.services.AddressService
import org.example.services.NotificationService
import org.example.services.WishlistItemService
import org.example.services.WishlistService

@Component
class AdminCustomerOperationsController @Inject constructor(
    private val addressService: AddressService,
    private val notificationService: NotificationService,
    private val wishlistService: WishlistService,
    private val wishlistItemService: WishlistItemService
) {
    fun Route.adminCustomerOperationRoutes() {
        route("/admin/customers") {
            get("addresses") {
                val userId = call.queryParameters["userId"]
                    ?: return@get respondAdminError("Customer addresses", "userId is required")
                respondAdminList("Customer addresses", addressService.listForUser(userId), "customers")
            }
            get("notifications") {
                val userId = call.queryParameters["userId"]
                    ?: return@get respondAdminError("Customer notifications", "userId is required")
                respondAdminList("Customer notifications", notificationService.getUserNotifications(userId), "customers")
            }
            get("wishlists") {
                val userId = call.queryParameters["userId"]
                    ?: return@get respondAdminError("Customer wishlists", "userId is required")
                respondAdminList("Customer wishlists", wishlistService.getWishlistsByUser(userId), "customers")
            }
            get("wishlist-items") {
                val wishlistId = call.queryParameters["wishlistId"]
                    ?: return@get respondAdminError("Wishlist items", "wishlistId is required")
                respondAdminList("Wishlist items", wishlistItemService.getWishlistItemsWithProductDetails(wishlistId), "customers")
            }
        }
    }
}
