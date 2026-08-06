package org.example.data.repo

import io.r2dbc.spi.ConnectionFactory
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import org.example.data.mappers.SettingsMapper
import org.example.di.Component
import org.example.di.Inject
import org.example.domain.models.content.Setting

@Component
class SettingRepository @Inject constructor(
    connectionFactory: ConnectionFactory,
    settingMapper: SettingsMapper
) : CrudRepository<Setting, String>(
    connectionFactory = connectionFactory,
    tableName = "settings",
    idColumn = "id",
    mapper = settingMapper
) {
    override val generatedColumns = listOf("id", "created_at", "updated_at")

    // Find setting by key
    suspend fun findByKey(key: String): Setting? {
        val sql = "SELECT * FROM $tableName WHERE key = :key"

        return connectionFactory.useConnection {
            createNamedStatement(sql, mapOf("key" to key))
                .execute()
                .awaitSingle()
                .map(rowMapper)
                .awaitFirstOrNull()
        }
    }

    // Get setting value by key
    suspend fun getValue(key: String): Map<String, Any>? {
        return findByKey(key)?.value
    }

    // Get a specific field from a setting's value
    suspend fun getValueField(key: String, field: String): Any? {
        return findByKey(key)?.value?.get(field)
    }

    // Upsert setting (create or update)
    suspend fun upsert(key: String, value: Map<String, Any>, description: String? = null): Setting {
        val existing = findByKey(key)

        return if (existing != null) {
            update(existing.id, existing.copy(
                value = value,
                description = description ?: existing.description
            )) ?: throw RuntimeException("Failed to update setting")
        } else {
            create(Setting(
                key = key,
                value = value,
                description = description
            ))
        }
    }

    // Search settings by key or description
    suspend fun search(query: String, limit: Int = 20): List<Setting> {
        val sql = """
            SELECT * FROM $tableName 
            WHERE key ILIKE :query 
               OR description ILIKE :query 
            ORDER BY key ASC 
            LIMIT :limit
        """.trimIndent()

        return executeQuery(sql, mapOf(
            "query" to "%$query%",
            "limit" to limit
        ))
    }

    // Get all settings as a map
    suspend fun getAllAsMap(): Map<String, Map<String, Any>> {
        val sql = "SELECT * FROM $tableName ORDER BY key ASC"

        return executeQuery<Setting>(sql)
            .associate { it.key to it.value }
    }

    // Bulk update settings
    suspend fun bulkUpdate(settings: Map<String, Map<String, Any>>): List<Setting> {
        return settings.map { (key, value) -> upsert(key, value) }
    }

    // Delete setting by key
    suspend fun deleteByKey(key: String): Boolean {
        val sql = "DELETE FROM $tableName WHERE key = :key"

        return connectionFactory.withTransaction { connection ->
            connection.createNamedStatement(sql, mapOf("key" to key))
                .execute()
                .awaitSingle()
                .rowsUpdated
                .awaitSingle() > 0
        }
    }

    // Get settings by prefix (e.g., "email.*")
    suspend fun findByPrefix(prefix: String): List<Setting> {
        val sql = """
            SELECT * FROM $tableName 
            WHERE key LIKE :prefix 
            ORDER BY key ASC
        """.trimIndent()

        return executeQuery(sql, mapOf("prefix" to "$prefix%"))
    }
}