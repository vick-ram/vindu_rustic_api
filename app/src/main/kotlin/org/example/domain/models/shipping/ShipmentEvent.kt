package org.example.domain.models.shipping

import com.google.gson.annotations.SerializedName
import kotlinx.datetime.LocalDateTime
import org.example.utils.now
import org.example.utils.shortUUID
import java.io.Serializable
import java.time.OffsetDateTime

data class ShipmentEvent(
    val id: String = shortUUID(),

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
