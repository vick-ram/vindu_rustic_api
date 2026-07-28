package org.example.domain.models.shipping

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.example.domain.validations.OneOf
import org.example.utils.Ulid
import java.time.OffsetDateTime

@Serializable
data class ShipmentEvent(
    val id: String = Ulid.generate(),

    @SerialName("shipment_id")
    val shipmentId: String,

    @OneOf("picked_up", "in_transit", "out_for_delivery", "delivered", "failed_attempt", "exception")
    @SerialName("event_type")
    val eventType: String,

    val status: String? = null,
    val location: String? = null,
    val description: String? = null,

    @Contextual
    @SerialName("occurred_at")
    val occurredAt: OffsetDateTime? = null,

    @Contextual
    @SerialName("created_at")
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
)
