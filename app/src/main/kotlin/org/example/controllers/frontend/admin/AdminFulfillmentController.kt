package org.example.controllers.frontend.admin

import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import org.example.di.Component
import org.example.di.Inject
import org.example.services.ShipmentService
import org.example.services.WarehouseService

@Component
class AdminFulfillmentController @Inject constructor(
    private val warehouseService: WarehouseService,
    private val shipmentService: ShipmentService
) {
    fun Route.adminFulfillmentRoutes() {
        route("/admin/fulfillment") {
            get("warehouses") {
                respondAdminList("Warehouses", warehouseService.getWarehouses(), "fulfillment")
            }
            get("warehouses/active") {
                respondAdminList("Active warehouses", warehouseService.getActiveWarehouses(), "fulfillment")
            }
            get("shipments") {
                respondAdminList("Shipments", shipmentService.getShipments(), "fulfillment")
            }
            get("shipments/in-transit") {
                respondAdminList("In-transit shipments", shipmentService.getInTransitShipments(), "fulfillment")
            }
            get("shipments/overdue") {
                respondAdminList("Overdue shipments", shipmentService.getOverdueShipments(), "fulfillment")
            }
            get("shipments/events") {
                val shipmentId = call.queryParameters["shipmentId"]
                    ?: return@get respondAdminError("Shipment events", "shipmentId is required")
                respondAdminList("Shipment events", shipmentService.getShipmentEvents(shipmentId), "fulfillment")
            }
            get("shipments/timeline") {
                val shipmentId = call.queryParameters["shipmentId"]
                    ?: return@get respondAdminError("Shipment timeline", "shipmentId is required")
                respondAdminList("Shipment timeline", shipmentService.getShipmentTimeline(shipmentId), "fulfillment")
            }
        }
    }
}
