package org.example.domain.models

import org.example.domain.validations.Validations
import java.io.Serializable
import java.time.LocalDateTime

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

data class CreateUser(
    val name: String,
    val email: String,
    val password: String,
    val active: Boolean = true,
) {
    fun validate(): CreateUser {
        Validations.validateNonEmpty(name, "Name")
        Validations.validateEmail(email)
        Validations.validatePassword(password)
        return this
    }
}


data class Role(
    val name: String,
    val description: String? = null,
) {
    fun validate(): Role {
        require(name.isNotBlank()) { "Role name cannot be blank." }
        return this
    }
}

data class Permission(
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
    val active: Boolean,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
) : Serializable

