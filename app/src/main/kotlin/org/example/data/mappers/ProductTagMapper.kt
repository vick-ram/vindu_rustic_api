package org.example.data.mappers

import io.r2dbc.spi.Row
import io.r2dbc.spi.RowMetadata
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.properties.Properties
import kotlinx.serialization.properties.encodeToMap
import org.example.domain.models.catalog.ProductTag
import org.example.domain.repo.RowMapper
import org.example.domain.repo.toModel

object ProductTagMapper : RowMapper<Row, ProductTag> {
    override fun toModel(
        row: Row,
        metadata: RowMetadata
    ): ProductTag {
        return row.toModel(metadata)
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun toRow(model: ProductTag): Map<String, Any?> {
        return Properties.encodeToMap(model)
    }

    override fun getId(model: ProductTag): Any =  model.productId

}