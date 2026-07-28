package org.example.domain.models.inventory

import io.ktor.http.*
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.example.domain.validations.NotBlank
import org.example.utils.Ulid
import java.time.OffsetDateTime

@Serializable
data class Warehouse(
    val id: String = Ulid.generate(),

    @field:NotBlank
    val name: String,

    @field:NotBlank
    @SerialName("address_id")
    val addressId: String? = null,

    @SerialName("is_active")
    val isActive: Boolean = true,

    @Contextual
    @SerialName(value = "created_at")
    val createdAt: OffsetDateTime = OffsetDateTime.now()
) {
    companion object {
        fun formParameters(parameters: Parameters): Warehouse {
            val name = parameters["name"] as String
            val addressId = parameters["address"] as String

            return Warehouse(name = name, addressId = addressId)
        }
    }
}