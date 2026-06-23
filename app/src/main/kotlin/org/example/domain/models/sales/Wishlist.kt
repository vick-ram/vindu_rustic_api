package org.example.domain.models.sales

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.example.data.db.config.Ulid
import org.example.utils.OffsetDateTimeSerializer
import java.time.OffsetDateTime

@Serializable
data class Wishlist(
    val id: String = Ulid.generate(),

    @SerialName("user_id")
    val userId: String,

    val name: String = "default",

    @SerialName("is_public")
    val isPublic: Boolean = false,

    @Serializable(with = OffsetDateTimeSerializer::class)
    @SerialName("created_at")
    val createdAt: OffsetDateTime = OffsetDateTime.now()
)
