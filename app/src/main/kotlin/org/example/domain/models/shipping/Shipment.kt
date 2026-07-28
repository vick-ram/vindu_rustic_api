package org.example.domain.models.shipping

import io.ktor.http.*
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.example.utils.Ulid
import java.math.BigDecimal
import java.time.OffsetDateTime

@Serializable
data class Shipment(
    val id: String = Ulid.generate(),

    @SerialName(value = "order_id")
    val orderId: String,

    @SerialName("warehouse_id")
    val warehouseId: String? = null,

    val courier: String? = null,

    @SerialName("service_level")
    val serviceLevel: String? = null,

    @SerialName(value = "tracking_number")
    val trackingNumber: String? = null,

    @SerialName("tracking_url")
    val trackingUrl: String? = null,

    @SerialName("shipping_label_url")
    val shippingLabelUrl: String? = null,

    @Contextual
    val cost: BigDecimal? = null,

    @Contextual
    @SerialName(value = "estimated_delivery_at")
    val estimatedDeliveryAt: OffsetDateTime? = null,

    @Contextual
    @SerialName(value = "shipped_at")
    val shippedAt: OffsetDateTime = OffsetDateTime.now(),

    @Contextual
    @SerialName(value = "delivered_at")
    val deliveredAt: OffsetDateTime
) {
    companion object {
        fun formParameters(parameters: Parameters): Shipment {
            val orderId = parameters["orderId"].toString()
            val courier = parameters["courier"].toString()
            val warehouseId = parameters["warehouseId"].toString()
            val trackingNumber = parameters["trackingNumber"].toString()
            val trackingUrl = parameters["trackingUrl"].toString()
            val shippingLabelUrl = parameters["shippingLabelUrl"].toString()
            val cost = parameters["cost"]?.toBigDecimal() ?: BigDecimal.ZERO
            val serviceLevel = parameters["serviceLevel"].toString()
            val shippedAt = parameters["shippedAt"]?.let { OffsetDateTime.parse(it) } as OffsetDateTime
            val estimatedDeliveryAt = parameters["estimatedDeliveryAt"]?.let { OffsetDateTime.parse(it) } as OffsetDateTime
            val deliveredAt = parameters["deliveredAt"]?.let { OffsetDateTime.parse(it) } as OffsetDateTime

            return Shipment(
                orderId = orderId,
                warehouseId = warehouseId,
                courier = courier,
                serviceLevel = serviceLevel,
                trackingNumber = trackingNumber,
                trackingUrl = trackingUrl,
                shippingLabelUrl = shippingLabelUrl,
                cost = cost,
                estimatedDeliveryAt = estimatedDeliveryAt,
                shippedAt = shippedAt,
                deliveredAt = deliveredAt
            )
        }
    }
}