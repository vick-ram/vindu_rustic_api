package org.example.domain.models.catalog

import io.ktor.http.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProductTag(
    @SerialName("product_id")
    val productId: String,

    @SerialName("tag_id")
    val tagId: String,
) {
    companion object {
        fun formParameters(parameters: Parameters): ProductTag {
            val productId = parameters["productId"].toString()
            val tagId = parameters["tagId"].toString()

            return ProductTag(
                productId = productId,
                tagId = tagId
            )
        }
    }
}
