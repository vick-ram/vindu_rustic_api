package org.example.domain.models.identity

import io.ktor.http.*
import io.ktor.websocket.*
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.example.domain.validations.Email
import org.example.domain.validations.NotBlank
import org.example.domain.validations.OneOf
import org.example.domain.validations.Password
import org.example.domain.validations.Phone
import org.example.utils.Ulid
import java.time.OffsetDateTime


data class TokenResponse(
    val type: String,
    val accessToken: String,
    val refreshToken: String,
)

@Serializable
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

@Serializable
data class User(
    val id: String = Ulid.generate(),

    @SerialName(value = "first_name")
    @field:NotBlank(message = "first name cannot be blank")
    val firstName: String? = null,

    @SerialName(value = "last_name")
    @field:NotBlank
    val lastName: String? = null,

    @field:Email
    val email: String,

    @field:Password
    val password: String,

    @field:Phone
    @SerialName("phone_number")
    val phoneNumber: String? = null,

    val avatarUrl: String? = null,

    @SerialName(value = "email_verified")
    val emailVerified: Boolean = false,

    @SerialName(value = "phone_verified")
    val phoneVerified: Boolean = false,

    @OneOf("active", "suspended", "deactivated")
    val status: String = "active",

    @SerialName("two_factor_enabled")
    val twoFactorEnabled : Boolean = false,

    @SerialName("two_factor_method")
    val twoFactorMethod: String? = null, // "app", "sms", "email"

    @SerialName("notification_preferences")
    val notificationPreferences: NotificationPreferences = NotificationPreferences(),

    @Contextual
    @SerialName(value = "last_login_at")
    val lastLoginAt: OffsetDateTime? = null,

    @Contextual
    @SerialName(value = "created_at")
    val createdAt: OffsetDateTime = OffsetDateTime.now(),

    @Contextual
    @SerialName(value = "updated_at")
    val updatedAt: OffsetDateTime = OffsetDateTime.now(),

    @Contextual
    @SerialName(value = "deleted_at")
    val deletedAt: OffsetDateTime? = null,
) {

    companion object {
        fun formParameters(parameters: Parameters): User {
            val firstName = parameters["firstName"].toString()
            val lastName = parameters["lastName"].toString()
            val email = parameters["email"].toString()
            val password = parameters["password"].toString()
            val phone = parameters["phoneNumber"].toString()

            return User(
                firstName = firstName,
                lastName = lastName,
                email = email,
                password = password,
                phoneNumber = phone
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
                    "phone" to user.phoneNumber,
                    "avatar" to user.avatarUrl,
                    "lastLoginAt" to user.lastLoginAt,
                    "createdAt" to user.createdAt,
                    "updatedAt" to user.updatedAt
                )
            }
    }
}

@Serializable
data class NotificationPreferences(
    @SerialName("email_enabled")
    val emailEnabled: Boolean = true,

    @SerialName("sms_enabled")
    val smsEnabled: Boolean = true,

    @SerialName("push_enabled")
    val pushEnabled: Boolean = true,

    @SerialName("in_app_enabled")
    val inAppEnabled: Boolean = true,

    @SerialName("security_alerts")
    val securityAlerts: Boolean = true,

    @SerialName("marketing_emails")
    val marketingEmails: Boolean = false
)

@Serializable
data class ChatUser(
    val userId: String,
    val sessionId: String,
    val socket: WebSocketSession,
    val username: String,
    val joinTime: Long = System.currentTimeMillis()
)


