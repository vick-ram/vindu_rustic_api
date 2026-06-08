package org.example.data.db.tables

import org.example.data.db.config.CustomTable
import org.example.domain.models.support.MessageStatus
import org.example.domain.models.support.MessageType
import org.example.data.db.config.PGEnum
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.datetime.datetime

object ConversationTable : CustomTable("conversations") {
    val lastMessage = reference("last_message_id", MessagesTable, onDelete = ReferenceOption.SET_NULL).nullable()
}

object ParticipantsTable : CustomTable("participants") {
    val conversation = reference("conversation_id", ConversationTable, onDelete = ReferenceOption.CASCADE)
    val user = reference("user_id", Users, onDelete = ReferenceOption.CASCADE)
    val joinedAt = datetime("joined_at")
    val leftAt = datetime("left_at")
    val isActive = bool("is_active").default(false)

    init {
        uniqueIndex(conversation, user)
        index(false, conversation, user)
    }
}

object MessagesTable : CustomTable("messages") {
    val conversation = reference("conversation_id", ConversationTable, onDelete = ReferenceOption.CASCADE)
    val sender = reference("sender_id", Users, onDelete = ReferenceOption.CASCADE)
    val messageText = text("message_text", eagerLoading = true)
    val messageType = customEnumeration(
        name = "message_type",
        sql = "MessageType",
        fromDb = { value -> MessageType.valueOf(value as String) },
        toDb = { PGEnum("MessageType", it) }
    ).default(MessageType.TEXT)
    val parentMessage = reference("parent_message_id", this, onDelete = ReferenceOption.SET_NULL).nullable()

        // File attachment
    val fileUrl = varchar("file_url", 255).nullable()
    val fileName = varchar("file_name", 255).nullable()
    val fileSize = long("file_size").nullable()

    init {
        index(false, conversation, createdAt)
        index(false, sender, createdAt)
        index(false, parentMessage)
    }
}

object MessageStatusTable: CustomTable("message_status") {
    val message = reference("message_id", MessagesTable, onDelete = ReferenceOption.CASCADE)
    val user = reference("user_id", Users, onDelete = ReferenceOption.CASCADE)
    val status = customEnumeration(
        name = "status",
        sql = "MessageStatus",
        fromDb = { value -> MessageStatus.valueOf(value as String) },
        toDb = { PGEnum("MessageStatus", it) }
    ).default(MessageStatus.SENT)

    init {
        uniqueIndex(message, user)
        index(false, user, status)
    }
}