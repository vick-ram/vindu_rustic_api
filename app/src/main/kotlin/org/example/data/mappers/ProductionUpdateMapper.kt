package org.example.data.mappers

import io.r2dbc.spi.Row
import io.r2dbc.spi.RowMetadata
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.properties.Properties
import kotlinx.serialization.properties.encodeToMap
import org.example.domain.models.production.ProductionUpdate
import org.example.domain.repo.RowMapper
import org.example.domain.repo.toModel

object ProductionUpdateMapper : RowMapper<Row, ProductionUpdate> {
    override fun toModel(
        row: Row,
        metadata: RowMetadata
    ): ProductionUpdate {
        return row.toModel(metadata)
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun toRow(model: ProductionUpdate): Map<String, Any?> {
        return Properties.encodeToMap(model)
    }

    override fun getId(model: ProductionUpdate): Any =  model.id

}