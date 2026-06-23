package org.example.domain.repo

import io.r2dbc.spi.RowMetadata
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.properties.Properties
import kotlinx.serialization.properties.decodeFromMap

interface RowMapper<Row, Model> {
    fun toModel(row: Row, metadata: RowMetadata): Model
    fun toRow(model: Model): Map<String, Any?>
    fun getId(model: Model): Any
}

@OptIn(ExperimentalSerializationApi::class)
inline fun <reified T: Any>  io.r2dbc.spi.Row.toModel(metadata: RowMetadata): T {
    val map = mutableMapOf<String, Any>()

    // Extract all columns dynamically from metadata
    metadata.columnMetadatas.forEach { column ->
        val snakeKey = column.name
        val value = this.get(snakeKey)

        if (value != null) {
            val camelKey =snakeKey.toCamelCase()
            map[camelKey] = value
        }
    }
    return Properties.decodeFromMap(map)
}

// Quick helper function to automatically convert "user_id" to "userId"
fun String.toCamelCase(): String {
    return this.split("_")
        .mapIndexed { index, part ->
            if (index == 0) part.lowercase() else part.lowercase().replaceFirstChar { it.uppercase() }
        }
        .joinToString("")
}