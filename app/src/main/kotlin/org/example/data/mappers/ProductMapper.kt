package org.example.data.mappers

import io.r2dbc.spi.Row
import io.r2dbc.spi.RowMetadata
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.properties.Properties
import kotlinx.serialization.properties.encodeToMap
import org.example.domain.models.catalog.Product
import org.example.domain.repo.RowMapper
import org.example.domain.repo.toModel

object ProductMapper : RowMapper<Row, Product> {
    override fun toModel(
        row: Row,
        metadata: RowMetadata
    ): Product {
        return row.toModel(metadata)
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun toRow(model: Product): Map<String, Any?> {
        return Properties.encodeToMap(model)
    }

    override fun getId(model: Product): Any = model.id

}

