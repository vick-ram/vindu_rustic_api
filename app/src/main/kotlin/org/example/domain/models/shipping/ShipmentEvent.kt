package org.example.domain.models.shipping

import com.google.gson.annotations.SerializedName
import org.example.data.db.config.Ulid
import java.io.Serializable
import java.time.OffsetDateTime

data class ShipmentEvent(
    val id: String = Ulid.generate(),

    @SerializedName("shipment_id")
    val shipmentId: String,

    @SerializedName("event_type")
    val eventType: String,

    val status: String? = null,
    val location: String? = null,
    val description: String? = null,

    @SerializedName("occurred_at")
    val occurredAt: OffsetDateTime? = null,

    @SerializedName("created_at")
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
): Serializable
