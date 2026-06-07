package org.example.data.db.tables

import org.example.data.db.config.CustomTable
import org.jetbrains.exposed.v1.datetime.datetime
import org.jetbrains.exposed.v1.datetime.timestampWithTimeZone

object ShipmentEvents : CustomTable("shipment_events") {
    val shipmentId = reference("shipment_id", Shipments)
    val eventType = varchar("event_type", 50)
    val status = varchar("status", 100).nullable()
    val location = varchar("location", 255).nullable()
    val description = text("description").nullable()
    val occurredAt = timestampWithTimeZone("occurred_at").nullable()
}