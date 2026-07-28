package org.example.domain.models.sales

import io.ktor.http.*
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.example.utils.Ulid
import java.time.OffsetDateTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Serializable
data class ShoppingCart @OptIn(ExperimentalUuidApi::class) constructor(
    val id: String = Ulid.generate(),

    @SerialName(value = "user_id")
    val userId: String? = null,

    @SerialName("guest_token")
    val guestToken: Uuid = Uuid.random(),

    @Contextual
    @SerialName(value = "created_at")
    val createdAt: OffsetDateTime = OffsetDateTime.now(),

    @Contextual
    @SerialName(value = "updated_at")
    val updatedAt: OffsetDateTime = OffsetDateTime.now()
) {
    companion object {
        @OptIn(ExperimentalUuidApi::class)
        fun formParameters(parameters: Parameters): ShoppingCart {
            return ShoppingCart(
                userId = parameters["user_id"] as String,
            )
        }
    }
}