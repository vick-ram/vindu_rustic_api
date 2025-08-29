package org.example.domain.models

import io.ktor.websocket.WebSocketSession
import kotlinx.datetime.LocalDateTime
import org.example.domain.validations.Validations
import org.example.utils.now
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
        Validations.validateEmail(email)
        return this
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
    val id: String,
    val name: String,
    val email: String,
    val password: String,
    val active: Boolean = true,
    val roleId: String,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
) : Serializable {

    fun validate(): User {
        Validations.validateAll(
            { Validations.validateNonEmpty(name, "Name") },
            { Validations.validateEmail(email) },
            { Validations.validatePassword(password) }
        )

        return this
    }
}

data class ChatUser(
    val userId: String,
    val sessionId: String,
    val socket: WebSocketSession,
    val username: String,
    val joinTime: Long = System.currentTimeMillis()
)


