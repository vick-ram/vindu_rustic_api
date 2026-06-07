package org.example.data.db.entities

import org.example.data.db.config.CustomEntity
import org.example.data.db.config.CustomEntityClass
import org.example.data.db.tables.Roles
import org.example.data.db.tables.UserRoles
import org.jetbrains.exposed.v1.core.dao.id.EntityID

class RoleEntity(id: EntityID<String>): CustomEntity(id, Roles) {
    companion object : CustomEntityClass<RoleEntity>(Roles)

    var name by Roles.name
    var description by Roles.description
    var tsv by Roles.tsv

    val users by UserEntity via UserRoles

}