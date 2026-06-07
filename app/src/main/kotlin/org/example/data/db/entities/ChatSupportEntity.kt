package org.example.data.db.entities

import org.example.data.db.config.CustomEntity
import org.example.data.db.config.CustomEntityClass
import org.example.data.db.tables.ConversationTable
import org.example.data.db.tables.MessageStatusTable
import org.example.data.db.tables.MessagesTable
import org.example.data.db.tables.ParticipantsTable
import org.jetbrains.exposed.v1.core.dao.id.EntityID

class ConversationEntity(id: EntityID<String>) : CustomEntity(id, ConversationTable) {
    companion object : CustomEntityClass<ConversationEntity>(ConversationTable)

//    var lastMessage by ConversationTable.lastMessage
    var lastMessage by MessageEntity optionalReferencedOn ConversationTable.lastMessage

    // Relationships
    val participants by ParticipantsEntity referrersOn ParticipantsTable.conversation
    val messages by MessageEntity referrersOn MessagesTable.conversation
}

class ParticipantsEntity(id: EntityID<String>) : CustomEntity(id, ParticipantsTable) {
    companion object : CustomEntityClass<ParticipantsEntity>(ParticipantsTable)

    var joinedAt by ParticipantsTable.joinedAt
    var leftAt by ParticipantsTable.leftAt
    var isActive by ParticipantsTable.isActive

    var conversation by ConversationEntity referencedOn ParticipantsTable.conversation
    var user by UserEntity referencedOn ParticipantsTable.user
}

class MessageEntity(id: EntityID<String>) : CustomEntity(id, MessagesTable) {
    companion object: CustomEntityClass<MessageEntity>(MessagesTable)

    var messageText by MessagesTable.messageText
    var messageType by MessagesTable.messageType

    var fileUrl by MessagesTable.fileUrl
    var fileName by MessagesTable.fileName
    var fileSize by MessagesTable.fileSize

    var conversation by ConversationEntity referencedOn MessagesTable.conversation
    var sender by UserEntity referencedOn MessagesTable.sender
    var parentMessage by MessageEntity optionalReferencedOn MessagesTable.parentMessage
}

class MessageStatusEntity(id: EntityID<String>) : CustomEntity(id, MessageStatusTable) {
    companion object : CustomEntityClass<MessageStatusEntity>(MessageStatusTable)

    var status by MessageStatusTable.status

    var message by MessageEntity referencedOn MessageStatusTable.message
    var user by UserEntity referencedOn MessageStatusTable.user
}