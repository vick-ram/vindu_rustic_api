package org.example.domain.models.identity

import io.ktor.http.*
import kotlinx.serialization.Serializable
import org.example.domain.validations.NotBlank
import org.example.utils.Ulid

@Serializable
data class Role(
    val id: String = Ulid.generate(),

    @field:NotBlank(message = "Name is required")
    val name: String,

    val description: String? = null,
) {
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