package org.example.data.db.tables

import org.example.data.db.config.CustomTable

object UserRoles: CustomTable("user_roles") {
    val userId = reference("user_id", Users)
    val roleId = reference("role_id", Roles)

    override val primaryKey = PrimaryKey(userId, roleId)

}