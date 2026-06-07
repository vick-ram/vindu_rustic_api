package org.example.data.db.tables

import org.example.data.db.config.CustomTable
import org.example.utils.tsVector
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.lowerCase
import org.jetbrains.exposed.v1.datetime.datetime
import org.jetbrains.exposed.v1.datetime.timestampWithTimeZone

object Users : CustomTable("users") {
    val firstName = varchar("first_name", 120).nullable()
    val lastName = varchar("last_name", 120).nullable()
    val email = varchar("email", 255).uniqueIndex()
    val password = varchar("password", 255)
    val phoneNumber = varchar("phone", 30).nullable()
    val avatarUrl = varchar("avatar_url", 100).nullable()
    val emailVerified = bool("email_verified").default(false)
    val phoneVerified = bool("phone_verified").default(false)
    val status = varchar("status", 15).default("active")
    val lastLoginAt = timestampWithTimeZone("last_login_at").nullable()
    val deletedAt = timestampWithTimeZone("deleted_at").nullable()
    val tsv = tsVector("tsv")
}
