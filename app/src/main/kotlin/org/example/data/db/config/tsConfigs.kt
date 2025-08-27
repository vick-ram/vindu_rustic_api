package org.example.data.db.config

data class TsVectorConfig(
    val tableName: String,
    val tsVectorColumn: String,
    val searchColumns: List<String>,
    val weights: Map<String, String> = emptyMap(),
    val language: String = "english"
)

val tsVectorConfigs = listOf(
    TsVectorConfig(
        tableName = "users",
        tsVectorColumn = "tsv",
        searchColumns = listOf("name", "email"),
        weights = mapOf("name" to "A", "email" to "B")
    )
)