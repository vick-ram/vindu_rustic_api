package org.example.data.mappers

import io.r2dbc.spi.Row
import io.r2dbc.spi.RowMetadata
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.properties.Properties
import kotlinx.serialization.properties.encodeToMap
import org.example.domain.models.identity.Address
import org.example.domain.repo.RowMapper
import org.example.domain.repo.toModel

object AddressMapper : RowMapper<Row, Address> {
    override fun toModel(row: Row, metadata: RowMetadata): Address {
        return row.toModel(metadata)
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun toRow(model: Address): Map<String, Any?> {
        return Properties.encodeToMap(model)
    }

    override fun getId(model: Address): Any = model.id
}