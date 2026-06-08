package org.example.domain.models.identity

import io.ktor.http.*
import org.example.data.db.config.Ulid
import org.example.domain.validations.NotBlank
import java.io.Serializable

data class Role(
    val id: String = Ulid.generate(),

    @field:NotBlank(message = "Name is required")
    val name: String,

    val description: String? = null,
): Serializable {
    companion object {
        fun formParameters(parameters: Parameters): Role {
            val name = parameters["name"].toString()
            val description = parameters["description"]

            return Role(
                name = name,
                description = description
            )
        }

        val columns: List<Map<String, Any>> = listOf(
            mapOf()
        )

        fun toRows(roles: List<Role>): List<Map<String, Any?>> =
            roles.map { role ->
                mapOf(
                    "id" to role.id,
                    "name" to role.name,
                    "description" to role.description
                )
            }
    }
}