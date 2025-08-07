package org.example.data.db.entities

import org.example.data.db.tables.PermissionTable
import org.example.data.db.tables.RolePermissionTable
import org.example.data.db.tables.RoleTable
import org.example.data.db.tables.UserTable
import org.example.utils.CustomEntity
import org.example.utils.CustomEntityClass
import org.jetbrains.exposed.v1.core.dao.id.EntityID

class UserEntity(id: EntityID<String>): CustomEntity(id, UserTable) {
    companion object : CustomEntityClass<UserEntity>(UserTable)

    var name by UserTable.name
    var email by UserTable.email
    var password by UserTable.password
    var active by UserTable.active

    var role by RoleEntity referencedOn UserTable.role

}

class RoleEntity(id: EntityID<String>): CustomEntity(id, RoleTable) {
    companion object : CustomEntityClass<RoleEntity>(RoleTable)

    var name by RoleTable.name
    var description by RoleTable.description

    val users by UserEntity referrersOn UserTable.role
    var permissions by PermissionEntity via RolePermissionTable
}

class PermissionEntity(id: EntityID<String>): CustomEntity(id, PermissionTable) {
    companion object : CustomEntityClass<PermissionEntity>(PermissionTable)

    var name by PermissionTable.name
    var description by PermissionTable.description

    var roles by RoleEntity via RolePermissionTable
}
