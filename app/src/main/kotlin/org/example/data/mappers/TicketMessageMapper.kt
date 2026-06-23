package org.example.data.mappers

import io.r2dbc.spi.Row
import io.r2dbc.spi.RowMetadata
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.properties.Properties
import kotlinx.serialization.properties.encodeToMap
import org.example.domain.models.support.TicketMessage
import org.example.domain.repo.RowMapper
import org.example.domain.repo.toModel

object TicketMessageMapper : RowMapper<Row, TicketMessage> {
    override fun toModel(
        row: Row,
        metadata: RowMetadata
    ): TicketMessage {
        return row.toModel(metadata)
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun toRow(model: TicketMessage): Map<String, Any?> {
        return Properties.encodeToMap(model)
    }

    override fun getId(model: TicketMessage): Any = model.id
}