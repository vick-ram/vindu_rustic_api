package org.example.data.mappers

import io.r2dbc.spi.Row
import io.r2dbc.spi.RowMetadata
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.properties.Properties
import kotlinx.serialization.properties.encodeToMap
import org.example.domain.models.payments.Refund
import org.example.domain.repo.RowMapper
import org.example.domain.repo.toModel

object RefundMapper : RowMapper<Row, Refund> {
    override fun toModel(
        row: Row,
        metadata: RowMetadata
    ): Refund {
        return row.toModel(metadata)
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun toRow(model: Refund): Map<String, Any?> {
        return Properties.encodeToMap(model)
    }

    override fun getId(model: Refund): Any = model.id

}