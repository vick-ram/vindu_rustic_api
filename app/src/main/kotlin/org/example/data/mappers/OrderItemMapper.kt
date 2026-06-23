package org.example.data.mappers

import io.r2dbc.spi.Row
import io.r2dbc.spi.RowMetadata
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.properties.Properties
import kotlinx.serialization.properties.encodeToMap
import org.example.domain.models.sales.OrderItem
import org.example.domain.repo.RowMapper
import org.example.domain.repo.toModel

object OrderItemMapper : RowMapper<Row, OrderItem> {
    override fun toModel(
        row: Row,
        metadata: RowMetadata
    ): OrderItem {
        return row.toModel(metadata)
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun toRow(model: OrderItem): Map<String, Any?> {
        return Properties.encodeToMap(model)
    }

    override fun getId(model: OrderItem): Any = model.id

}