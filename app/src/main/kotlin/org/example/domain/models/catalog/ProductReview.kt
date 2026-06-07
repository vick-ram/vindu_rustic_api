package org.example.domain.models.catalog

import com.google.gson.annotations.SerializedName
import kotlinx.datetime.LocalDateTime
import org.example.data.db.entities.ProductEntity
import org.example.data.db.entities.UserEntity
import org.example.data.mappers.ProductMapper
import org.example.data.mappers.UserMapper
import org.example.domain.models.identity.User
import org.example.domain.validations.Between
import org.example.domain.validations.NotBlank
import org.example.plugins.NotFoundException
import org.example.utils.toCustomFormat
import java.io.Serializable
import java.time.OffsetDateTime

data class ProductReview(
    val id: String,

    @SerializedName(value = "product_id")
    val productId: String,

    @SerializedName(value = "user_id")
    val userId: String,

    @SerializedName(value = "order_item_id")
    val orderItemId: String,

    @field:Between(min = 1, max = 5)
    val rating: Int, // 1-5
    val title: String,
    val review: String,

    @SerializedName(value = "is_verified_purchase")
    val isVerifiedPurchase: Boolean = false,

    @SerializedName(value = "created_at")
    val createdAt: OffsetDateTime = OffsetDateTime.now()
): Serializable
