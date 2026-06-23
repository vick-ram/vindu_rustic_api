package org.example.data.mappers

import io.r2dbc.spi.Row
import io.r2dbc.spi.RowMetadata
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.properties.Properties
import kotlinx.serialization.properties.encodeToMap
import org.example.domain.models.system.DeviceToken
import org.example.domain.models.system.Notification
import org.example.domain.repo.RowMapper
import org.example.domain.repo.toModel

object NotificationMapper : RowMapper<Row, Notification> {
    override fun toModel(
        row: Row,
        metadata: RowMetadata
    ): Notification {
        return row.toModel(metadata)
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun toRow(model: Notification): Map<String, Any?> {
        return Properties.encodeToMap(model)
    }

    override fun getId(model: Notification): Any =model.id

}

object DeviceTokenMapper : RowMapper<Row, DeviceToken> {
    override fun toModel(
        row: Row,
        metadata: RowMetadata
    ): DeviceToken {
        return row.toModel(metadata)
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun toRow(model: DeviceToken): Map<String, Any?> {
        return Properties.encodeToMap(model)
    }

    override fun getId(model: DeviceToken): Any = model.id

}