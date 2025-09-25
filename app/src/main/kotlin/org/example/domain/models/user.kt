package org.example.domain.models

import io.ktor.http.*
import io.ktor.websocket.*
import kotlinx.datetime.LocalDateTime
import org.example.domain.validations.Validations
import org.example.utils.now
import org.example.utils.shortUUID
import org.example.utils.toCustomFormat
import java.io.Serializable

data class TokenResponse(
    val type: String,
    val token: String
)

data class LoginCredentials(
    val email: String,
    val password: String
) {
    fun validate(): LoginCredentials {
        Validations.validateAll(
            { Validations.validateEmail(email) },
            { Validations.validatePassword(password) }
        )
        return this
    }

    companion object {
        fun formParameters(parameters: Parameters): LoginCredentials {
            val email = parameters["email"].toString()
            val password = parameters["password"].toString()

            return LoginCredentials(email, password)
        }
    }

}


data class Role(
    val id: String,
    val name: String,
    val description: String? = null,
) {
    fun validate(): Role {
        Validations.validateNonEmpty(name, "Role name cannot be blank.")
        return this
    }
}

data class Permission(
    val id: String,
    val name: String,
    val description: String? = null,
) {
    fun validate(): Permission {
        Validations.validateNonEmpty(name, "Name")
        return this
    }
}

data class User(
    val id: String = shortUUID(),
    val name: String,
    val email: String,
    val password: String,
    val active: Boolean = true,
    val roleId: String,
    val avatar: String? = null,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) : Serializable {

    fun validate(): User {
        Validations.validateAll(
            { Validations.validateNonEmpty(name, "Name") },
            { Validations.validateEmail(email) },
            { Validations.validatePassword(password) }
        )

        return this
    }

    companion object {
        fun formParameters(parameters: Parameters): User {
            val name = parameters["name"].toString()
            val email = parameters["email"].toString()
            val password = parameters["password"].toString()
            val roleId = parameters["roleId"] ?: ""

            return User(
                name = name,
                email = email,
                password = password,
                roleId = roleId,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )
        }

        val columns: List<Map<String, Any>> = listOf(
            mapOf("key" to "id", "label" to "ID"),
            mapOf("key" to "name", "label" to "Name", "sortable" to true),
            mapOf("key" to "email", "label" to "Email", "sortable" to true),
            mapOf("key" to "status", "label" to "Status", "sortable" to true),
            mapOf("key" to "createdAt", "label" to "Created At", "sortable" to true),
            mapOf("key" to "updatedAt", "label" to "Updated At", "sortable" to true),
        )

        fun toRows(users: List<User>): List<Map<String, Any?>> =
            users.map { user ->
                mapOf(
                    "id" to user.id,
                    "name" to user.name,
                    "email" to user.email,
                    "active" to user.active,
                    "avatar" to user.avatar,
                    "createdAt" to user.createdAt.toCustomFormat(),
                    "updatedAt" to user.updatedAt.toCustomFormat()
                )
            }
    }
}

data class ChatUser(
    val userId: String,
    val sessionId: String,
    val socket: WebSocketSession,
    val username: String,
    val joinTime: Long = System.currentTimeMillis()
)


