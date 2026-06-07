package org.example.domain.models.shipping

import com.google.gson.annotations.SerializedName
import io.ktor.http.Parameters
import kotlinx.datetime.LocalDateTime
import org.example.utils.now
import org.example.utils.shortUUID
import java.io.Serializable
import java.math.BigDecimal
import java.time.OffsetDateTime

data class Shipment(
    val id: String = shortUUID(),

    @SerializedName(value = "order_id")
    val orderId: String,

    @SerializedName("warehouse_id")
    val warehouseId: String? = null,

    val courier: String? = null,

    @SerializedName("service_level")
    val serviceLevel: String? = null,

    @SerializedName(value = "tracking_number")
    val trackingNumber: String? = null,

    @SerializedName("tracking_number")
    val trackingUrl: String? = null,

    @SerializedName("shipping_label_url")
    val shippingLabelUrl: String? = null,

    val cost: BigDecimal? = null,

    @SerializedName(value = "estimated_delivery_at")
    val estimatedDeliveryAt: OffsetDateTime? = null,

    @SerializedName(value = "shipped_at")
    val shippedAt: OffsetDateTime = OffsetDateTime.now(),

    @SerializedName(value = "delivered_at")
    val deliveredAt: OffsetDateTime
): Serializable {
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
            val shippedAt = parameters["shippedAt"]?.let { LocalDateTime.parse(it) } as LocalDateTime
            val estimatedDeliveryAt = parameters["estimatedDeliveryAt"]?.let { LocalDateTime.parse(it) } as LocalDateTime
            val deliveredAt = parameters["deliveredAt"]?.let { LocalDateTime.parse(it) } as LocalDateTime

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