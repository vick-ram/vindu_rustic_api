package org.example.data.mappers

import io.r2dbc.spi.Row
import io.r2dbc.spi.RowMetadata
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.properties.Properties
import kotlinx.serialization.properties.encodeToMap
import org.example.domain.models.inventory.InventoryMovement
import org.example.domain.repo.RowMapper
import org.example.domain.repo.toModel

object InventoryMovementMapper : RowMapper<Row, InventoryMovement> {
    override fun toModel(
        row: Row,
        metadata: RowMetadata
    ): InventoryMovement {
        return row.toModel(metadata)
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun toRow(model: InventoryMovement): Map<String, Any?> {
        return Properties.encodeToMap(model)
    }

    override fun getId(model: InventoryMovement): Any = model.id

}