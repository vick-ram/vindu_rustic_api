package org.example.data.mappers

import io.r2dbc.spi.Row
import io.r2dbc.spi.RowMetadata
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.properties.Properties
import kotlinx.serialization.properties.encodeToMap
import org.example.domain.models.sales.Wishlist
import org.example.domain.repo.RowMapper
import org.example.domain.repo.toModel

object WishlistMapper : RowMapper<Row, Wishlist> {
    override fun toModel(
        row: Row,
        metadata: RowMetadata
    ): Wishlist {
        return row.toModel(metadata)
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun toRow(model: Wishlist): Map<String, Any?> {
        return Properties.encodeToMap(model)
    }

    override fun getId(model: Wishlist): Any = model.id
}