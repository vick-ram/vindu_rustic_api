package org.example.data.db.tables

import org.example.data.db.config.CustomTable

object CustomProductAttachments : CustomTable("custom_product_attachments") {
    val requestId = reference("request_id", CustomProductRequests)
    val fileUrl = text("file_url")
    val fileType = varchar("file_type", 50).nullable()
    val fileSizeBytes = long("file_size_bytes").nullable()
}