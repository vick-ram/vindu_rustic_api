package org.example.data.mappers

import io.r2dbc.spi.Row
import io.r2dbc.spi.RowMetadata
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.properties.Properties
import kotlinx.serialization.properties.encodeToMap
import org.example.domain.models.marketing.Coupon
import org.example.domain.repo.RowMapper
import org.example.domain.repo.toModel

object CouponMapper : RowMapper<Row, Coupon> {
    override fun toModel(
        row: Row,
        metadata: RowMetadata
    ): Coupon {
        return row.toModel(metadata)
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun toRow(model: Coupon): Map<String, Any?> {
        return Properties.encodeToMap(model)
    }

    override fun getId(model: Coupon): Any = model.id

}