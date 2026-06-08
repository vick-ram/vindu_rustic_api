package org.example.domain.models.catalog

import com.google.gson.annotations.SerializedName
import io.ktor.http.Parameters
import java.io.Serializable

data class ProductTag(
    @SerializedName("product_id")
    val productId: String,

    @SerializedName("tag_id")
    val tagId: String,
): Serializable {
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
