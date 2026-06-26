package org.example.data.mappers

import io.r2dbc.spi.Row
import io.r2dbc.spi.RowMetadata
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import org.example.domain.repo.RowMapper
import org.example.domain.repo.toMap
import org.example.domain.repo.toModel

abstract class BaseRowMapper<Model: Any>(
    private val serializer: KSerializer<Model>,
    private val idExtractor: (Model) -> Any
): RowMapper<Row, Model> {
    override fun toModel(row: Row, metadata: RowMetadata): Model {
        return row.toModel(metadata, serializer)
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun toRow(model: Model): Map<String, Any?> {
        return model.toMap(serializer)
    }

    override fun getId(model: Model): Any = idExtractor(model)
}