package org.example.data.db.config

data class TsVectorConfig(
    val tableName: String,
    val tsVectorColumn: String,
    val searchColumns: List<String>,
    val weights: Map<String, String> = emptyMap(),
    val enumColumns: Set<String> = emptySet(),
    val language: String = "english",
    val indexName: String? = null
) {
    init {
        require(searchColumns.isNotEmpty()) { "searchColumns cannot be empty" }
        require(weights.keys.containsAll(searchColumns)) { "All searchColumns must have weights defined" }
    }

    val actualIndexName: String get() = indexName ?: "${tableName}_${tsVectorColumn}_idx"
}

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
        weights = mapOf("name" to "A", "sku" to "B", "short_description" to "C", "description" to "D")
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
        weights = mapOf("name" to "A", "description" to "B", "type" to "C", "applied_to" to "D"),
        enumColumns = setOf("type", "applied_to")
    ),
    TsVectorConfig(
        tableName = "special_offers",
        tsVectorColumn = "tsv",
        searchColumns = listOf("name", "description", "type"),
        weights = mapOf("name" to "A", "description" to "B", "type" to "C"),
        enumColumns = setOf("type")
    ),
    TsVectorConfig(
        tableName = "orders",
        tsVectorColumn = "tsv",
        searchColumns = listOf("order_number", "status", "payment_status"),
        weights = mapOf("order_number" to "A", "status" to "B", "payment_status" to "C"),
        enumColumns = setOf("status", "payment_status")
    ),
)