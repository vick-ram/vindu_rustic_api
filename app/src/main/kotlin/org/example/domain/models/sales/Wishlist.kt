package org.example.domain.models.sales

import com.google.gson.annotations.SerializedName
import kotlinx.datetime.LocalDateTime
import org.example.utils.now
import org.example.utils.shortUUID
import java.io.Serializable
import java.time.OffsetDateTime

data class Wishlist(
    val id: String = shortUUID(),

    @SerializedName("user_id")
    val userId: String,

    val name: String = "default",

    @SerializedName("is_public")
    val isPublic: Boolean = false,

    @SerializedName("created_at")
    val createdAt: OffsetDateTime = OffsetDateTime.now()
): Serializable
