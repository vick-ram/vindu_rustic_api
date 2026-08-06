package org.example.data.repo

import io.r2dbc.spi.ConnectionFactory
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import org.example.data.mappers.CustomProductAttachmentMapper
import org.example.di.Component
import org.example.di.Inject
import org.example.domain.models.customization.CustomProductAttachment

@Component
class CustomProductAttachmentRepository @Inject constructor(
    connectionFactory: ConnectionFactory,
    customProductAttachmentMapper: CustomProductAttachmentMapper
) : CrudRepository<CustomProductAttachment, String>(
    connectionFactory = connectionFactory,
    tableName = "custom_product_attachments",
    mapper = customProductAttachmentMapper
) {
    override val generatedColumns = listOf("id", "created_at")

    // Get attachments for a request
    suspend fun findByRequestId(requestId: String): List<CustomProductAttachment> {
        val sql = """
            SELECT * FROM $tableName 
            WHERE request_id = :requestId 
            ORDER BY created_at ASC
        """.trimIndent()

        return executeQuery(sql, mapOf("requestId" to requestId))
    }

    // Get attachments by file type
    suspend fun findByRequestAndType(
        requestId: String,
        fileType: String
    ): List<CustomProductAttachment> {
        val sql = """
            SELECT * FROM $tableName 
            WHERE request_id = :requestId AND file_type = :fileType 
            ORDER BY created_at ASC
        """.trimIndent()

        return executeQuery(
            sql, mapOf(
                "requestId" to requestId,
                "fileType" to fileType
            )
        )
    }

    // Get total file size for a request
    suspend fun getTotalFileSize(requestId: String): Long {
        val sql = """
            SELECT COALESCE(SUM(file_size), 0) as total_size 
            FROM $tableName 
            WHERE request_id = :requestId
        """.trimIndent()

        return connectionFactory.useConnection {
            createNamedStatement(sql, mapOf("requestId" to requestId))
                .execute()
                .awaitSingle()
                .map { row, _ -> row.get("total_size", Long::class.java) ?: 0L }
                .awaitFirstOrNull() ?: 0L
        }
    }

    // Bulk create attachments
    suspend fun bulkCreate(attachments: List<CustomProductAttachment>): List<CustomProductAttachment> {
        if (attachments.isEmpty()) return emptyList()

        val columns = listOf("request_id", "file_url", "file_type", "file_size")
        val placeholders = List(attachments.size) { index ->
            "(${columns.joinToString(", ") { ":${it}_$index" }})"
        }

        val sql = """
            INSERT INTO $tableName (${columns.joinToString(", ")})
            VALUES ${placeholders.joinToString(", ")}
            RETURNING *
        """.trimIndent()

        val params = mutableMapOf<String, Any?>()

        attachments.forEachIndexed { index, attachment ->
            params["request_id_$index"] = attachment.requestId
            params["file_url_$index"] = attachment.fileUrl
            params["file_type_$index"] = attachment.fileType
            params["file_size_$index"] = attachment.fileSize
        }

        return connectionFactory.withTransaction { connection ->
            connection.createNamedStatement(sql, params)
            .execute()
                .awaitSingle()
                .map(rowMapper)
                .asFlow()
                .toList()
        }
    }

    // Delete all attachments for a request
    suspend fun deleteByRequestId(requestId: String): Int {
        val sql = "DELETE FROM $tableName WHERE request_id = :requestId"

        return connectionFactory.withTransaction { connection ->
            connection.createNamedStatement(sql, mapOf("requestId" to requestId))
                .execute()
                .awaitSingle()
                .rowsUpdated
                .awaitSingle()
                .toInt()
        }
    }

    // Check if attachment exists for a request
    suspend fun existsForRequest(requestId: String, fileUrl: String): Boolean {
        val sql = """
            SELECT COUNT(*) as count 
            FROM $tableName 
            WHERE request_id = :requestId AND file_url = :fileUrl
        """.trimIndent()

        return connectionFactory.useConnection {
            createNamedStatement(sql, mapOf("requestId" to requestId, "file_url" to fileUrl))
                .execute()
                .awaitSingle()
                .map { row, _ -> row.get("count", Long::class.java)!! > 0 }
                .awaitFirstOrNull() ?: false
        }
    }
}