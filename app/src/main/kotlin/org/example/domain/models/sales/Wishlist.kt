package org.example.domain.models.sales

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.example.utils.Ulid
import java.time.OffsetDateTime

@Serializable
data class Wishlist(
    val id: String = Ulid.generate(),

    @SerialName("user_id")
    val userId: String,

    val name: String = "default",

    @SerialName("is_public")
    val isPublic: Boolean = false,

    @Contextual
    @SerialName("created_at")
    val createdAt: OffsetDateTime = OffsetDateTime.now()
)
