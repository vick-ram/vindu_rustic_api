package org.example.data.mappers

import io.r2dbc.spi.Row
import io.r2dbc.spi.RowMetadata
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.properties.Properties
import kotlinx.serialization.properties.encodeToMap
import org.example.domain.models.shipping.ShipmentEvent
import org.example.domain.repo.RowMapper
import org.example.domain.repo.toModel

object ShipmentEventMapper : RowMapper<Row, ShipmentEvent> {
    override fun toModel(
        row: Row,
        metadata: RowMetadata
    ): ShipmentEvent {
        return row.toModel(metadata)
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun toRow(model: ShipmentEvent): Map<String, Any?> {
        return Properties.encodeToMap(model)
    }

    override fun getId(model: ShipmentEvent): Any  = model.id
}