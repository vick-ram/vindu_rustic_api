package org.example.domain.models.inventory

import com.google.gson.annotations.SerializedName
import io.ktor.http.*
import org.example.data.db.config.Ulid
import org.example.domain.validations.NotBlank
import java.io.Serializable
import java.time.OffsetDateTime

data class Warehouse(
    val id: String = Ulid.generate(),

    @field:NotBlank
    val name: String,

    @field:NotBlank
    @SerializedName("address_id")
    val addressId: String? = null,

    @SerializedName("is_active")
    val isActive: Boolean = true,

    @SerializedName(value = "created_at")
    val createdAt: OffsetDateTime = OffsetDateTime.now()
): Serializable {
    companion object {
        fun formParameters(parameters: Parameters): Warehouse {
            val name = parameters["name"] as String
            val addressId = parameters["address"] as String

            return Warehouse(name = name, addressId = addressId)
        }
    }
}