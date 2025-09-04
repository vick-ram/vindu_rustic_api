package org.example.data.db.tables

import org.example.utils.CustomTable
import org.example.utils.tsVector
import org.jetbrains.exposed.v1.core.ReferenceOption

object UserTable : CustomTable("users") {
    val name = varchar("name", 255)
    val email = varchar("email", 255)
    val password = varchar("password", 255)
    val active = bool("active")
    val tsv = tsVector("tsv")

    val role = reference("role", RoleTable, ReferenceOption.CASCADE)
}

object RoleTable : CustomTable("roles") {
    val name = varchar("name", 255).index()
    val description = varchar("description", 255).nullable()
    val tsv = tsVector("tsv")
}

object PermissionTable : CustomTable("permissions") {
    val name = varchar("name", 255)
    val description = varchar("description", 255).nullable()
}

object RolePermissionTable : CustomTable("role_permissions") {
    val role = reference("role", RoleTable, ReferenceOption.CASCADE)
    val permission = reference("permission", PermissionTable, ReferenceOption.CASCADE)

    override val primaryKey: PrimaryKey?
        get() = PrimaryKey(role, permission)
}

object AddressTable : CustomTable("addresses") {
    val user = reference("user", UserTable, ReferenceOption.CASCADE)
    val fullName = varchar("full_name", 100).index()
    val phone = varchar("phone", 20)
    val email = varchar("email", 100).nullable()
    val street = varchar("street", 255).index()
    val county = varchar("county", 100).index()
    val region = varchar("region", 100).index()
    val postalCode = varchar("postal_code", 20).index()
    val tsv = tsVector("tsv")
}