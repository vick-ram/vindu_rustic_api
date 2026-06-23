package org.example.data.mappers

import io.r2dbc.spi.Row
import io.r2dbc.spi.RowMetadata
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.properties.Properties
import kotlinx.serialization.properties.encodeToMap
import org.example.domain.models.catalog.ProductReview
import org.example.domain.repo.RowMapper
import org.example.domain.repo.toModel

object ProductReviewMapper : RowMapper<Row, ProductReview> {
    override fun toModel(
        row: Row,
        metadata: RowMetadata
    ): ProductReview {
        return row.toModel(metadata)
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun toRow(model: ProductReview): Map<String, Any?> {
        return Properties.encodeToMap(model)
    }

    override fun getId(model: ProductReview): Any = model.id

}