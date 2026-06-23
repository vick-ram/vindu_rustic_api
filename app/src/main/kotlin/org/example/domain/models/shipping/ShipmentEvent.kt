package org.example.domain.models.shipping

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.example.data.db.config.Ulid
import org.example.utils.OffsetDateTimeSerializer
import java.time.OffsetDateTime

@Serializable
data class ShipmentEvent(
    val id: String = Ulid.generate(),

    @SerialName("shipment_id")
    val shipmentId: String,

    @SerialName("event_type")
    val eventType: String,

    val status: String? = null,
    val location: String? = null,
    val description: String? = null,

    @Serializable(with = OffsetDateTimeSerializer::class)
    @SerialName("occurred_at")
    val occurredAt: OffsetDateTime? = null,

    @Serializable(with = OffsetDateTimeSerializer::class)
    @SerialName("created_at")
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
)
