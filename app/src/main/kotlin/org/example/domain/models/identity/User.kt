package org.example.domain.models.identity

import com.google.gson.annotations.SerializedName
import io.ktor.http.*
import io.ktor.websocket.*
import kotlinx.datetime.LocalDateTime
import org.example.domain.validations.Email
import org.example.domain.validations.NotBlank
import org.example.domain.validations.Password
import org.example.domain.validations.Phone
import org.example.utils.now
import org.example.utils.shortUUID
import org.example.utils.toCustomFormat
import java.io.Serializable
import java.time.OffsetDateTime

data class TokenResponse(
    val type: String,
    val accessToken: String,
    val refreshToken: String,
)

data class LoginCredentials(
    @field:Email
    val email: String,

    @field:Password
    val password: String
) {

    companion object {
        fun formParameters(parameters: Parameters): LoginCredentials {
            val email = parameters["email"].toString()
            val password = parameters["password"].toString()

            return LoginCredentials(email, password)
        }
    }

}

data class User(
    val id: String = shortUUID(),

    @SerializedName(value = "first_name")
    @field:NotBlank(message = "first name cannot be blank")
    val firstName: String? = null,

    @SerializedName(value = "last_name")
    @field:NotBlank
    val lastName: String? = null,

    @field:Email
    val email: String,

    @field:Password
    val password: String,

    @field:Phone
    @SerializedName("phone_number")
    val phoneNumber: String? = null,

    val avatarUrl: String? = null,

    @SerializedName(value = "email_verified")
    val emailVerified: Boolean = false,

    @SerializedName(value = "phone_verified")
    val phoneVerified: Boolean = false,

    val status: String = "active",

    @SerializedName(value = "last_login_at")
    val lastLoginAt: OffsetDateTime? = null,

    @SerializedName(value = "created_at")
    val createdAt: OffsetDateTime = OffsetDateTime.now(),

    @SerializedName(value = "updated_at")
    val updatedAt: OffsetDateTime = OffsetDateTime.now(),

    @SerializedName(value = "deleted_at")
    val deletedAt: OffsetDateTime? = null,
) : Serializable{

    companion object {
        fun formParameters(parameters: Parameters): User {
            val firstName = parameters["firstName"].toString()
            val lastName = parameters["lastName"].toString()
            val email = parameters["email"].toString()
            val password = parameters["password"].toString()
            val phone = parameters["phone"].toString()

            return User(
                firstName = firstName,
                lastName = lastName,
                email = email,
                password = password,
                phone = phone,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )
        }

        val columns: List<Map<String, Any>> = listOf(
            mapOf("key" to "id", "label" to "ID"),
            mapOf("key" to "name", "label" to "Name", "sortable" to true),
            mapOf("key" to "email", "label" to "Email", "sortable" to true),
            mapOf("key" to "phone", "label" to "Phone", "sortable" to true),
            mapOf("key" to "status", "label" to "Status", "sortable" to true),
            mapOf("key" to "lastLoginAt", "label" to "Last Login At", "sortable" to true),
            mapOf("key" to "createdAt", "label" to "Created At", "sortable" to true),
            mapOf("key" to "updatedAt", "label" to "Updated At", "sortable" to true),
        )

        fun toRows(users: List<User>): List<Map<String, Any?>> =
            users.map { user ->
                mapOf(
                    "id" to user.id,
                    "name" to "${user.firstName} ${user.lastName}",
                    "email" to user.email,
                    "phone" to user.phone,
                    "avatar" to user.avatar,
                    "lastLoginAt" to user.lastLoginAt?.toCustomFormat(),
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


