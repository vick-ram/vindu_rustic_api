package org.example.domain.models.sales

import com.google.gson.annotations.SerializedName
import io.ktor.http.Parameters
import kotlinx.datetime.LocalDateTime
import org.example.utils.now
import org.example.utils.shortUUID
import java.io.Serializable
import java.time.OffsetDateTime
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

data class ShoppingCart @OptIn(ExperimentalUuidApi::class) constructor(
    val id: String = shortUUID(),

    @SerializedName(value = "user_id")
    val userId: String? = null,

    @SerializedName("guest_token")
    val guestToken: UUID = UUID.randomUUID(),

    @SerializedName(value = "created_at")
    val createdAt: OffsetDateTime = OffsetDateTime.now(),

    @SerializedName(value = "updated_at")
    val updatedAt: OffsetDateTime = OffsetDateTime.now()
) : Serializable {
    companion object {
        fun formParameters(parameters: Parameters): ShoppingCart {
            return ShoppingCart(
                userId = parameters["user_id"] as String,
            )
        }
    }
}