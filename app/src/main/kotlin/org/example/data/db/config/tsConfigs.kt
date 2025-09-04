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
    ),
    TsVectorConfig(
        tableName = "roles",
        tsVectorColumn = "tsv",
        searchColumns = listOf("name", "description"),
        weights = mapOf("name" to "A", "description" to "B")
    ),
    TsVectorConfig(
        tableName = "categories",
        tsVectorColumn = "tsv",
        searchColumns = listOf("name", "slug", "description"),
        weights = mapOf("name" to "A", "slug" to "B", "description" to "C")
    ),
    TsVectorConfig(
        tableName = "products",
        tsVectorColumn = "tsv",
        searchColumns = listOf("name", "sku", "short_description", "description"),
        weights = mapOf("name" to "A", "sku" to "B", "shortDescription" to "C", "description" to "D")
    ),
    TsVectorConfig(
        tableName = "product_reviews",
        tsVectorColumn = "tsv",
        searchColumns = listOf("title"),
        weights = mapOf("title" to "A")
    ),
    TsVectorConfig(
        tableName = "discounts",
        tsVectorColumn = "tsv",
        searchColumns = listOf("name", "description", "type", "applied_to"),
        weights = mapOf("name" to "A", "description" to "B", "type" to "C", "applied_to" to "D")
    ),
    TsVectorConfig(
        tableName = "special_offers",
        tsVectorColumn = "tsv",
        searchColumns = listOf("name", "description", "type"),
        weights = mapOf("name" to "A", "description" to "B", "type" to "C")
    ),
    TsVectorConfig(
        tableName = "orders",
        tsVectorColumn = "tsv",
        searchColumns = listOf("order_number", "status", "payment_status"),
        weights = mapOf("order_number" to "A", "status" to "B", "payment_status" to "C")
    ),
)