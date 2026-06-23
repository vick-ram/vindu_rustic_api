package org.example.domain.models.identity

import io.ktor.http.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.example.data.db.config.Ulid
import org.example.domain.validations.NotBlank
import org.example.utils.BigDecimalSerializer
import org.example.utils.OffsetDateTimeSerializer
import java.math.BigDecimal
import java.time.OffsetDateTime

@Serializable
data class Address(
    val id: String = Ulid.generate(),

    @SerialName(value = "user_id")
    val userId: String? = null,

    val label: String? = null,

    @field:NotBlank(message = "Name cannot be blank")
    @SerialName("recipient_name")
    val recipientName: String,

    @field:NotBlank(message = "Phone cannot be blank")
    @SerialName("phone_number")
    val phoneNumber: String,

    @SerialName(value = "country_code")
    val countryCode: String = "+254",

    val country: String = "kenya",
    val city: String? = null,
    val state: String? = null,

    @SerialName(value = "postal_code")
    val postalCode: String? = null,

    @SerialName(value = "address_line1")
    val addressLine1: String,

    @SerialName(value = "address_line2")
    val addressLine2: String? = null,

    @Serializable(with = BigDecimalSerializer::class)
    val latitude: BigDecimal? = null,

    @Serializable(with = BigDecimalSerializer::class)
    val longitude: BigDecimal? = null,

    @SerialName(value = "is_default")
    val isDefault: Boolean = false,

    @Serializable(with = OffsetDateTimeSerializer::class)
    @SerialName(value = "created_at")
    val createdAt: OffsetDateTime = OffsetDateTime.now()
) {
    companion object {
        fun formParameters(parameters: Parameters): Address {
            val userId = parameters["user_id"].toString()
            val label = parameters["label"].toString()
            val recipientName = parameters["recipient_name"].toString()
            val phoneNumber = parameters["phone_number"].toString()
            val countryCode = parameters["country_code"].toString()
            val country = parameters["country"] ?: "kenya"
            val city = parameters["city"].toString()
            val state = parameters["state"].toString()
            val postalCode = parameters["postal_code"].toString()
            val addressLine1 = parameters["address_line1"].toString()
            val addressLine2 = parameters["address_line2"]
            val latitude = parameters["latitude"]?.toBigDecimalOrNull()
            val longitude = parameters["longitude"]?.toBigDecimalOrNull()
            val isDefault = parameters["is_default"]?.toBooleanStrictOrNull() ?: false

            return Address(
                userId = userId,
                label = label,
                recipientName = recipientName,
                phoneNumber = phoneNumber,
                countryCode = countryCode,
                country = country,
                city = city,
                state = state,
                postalCode = postalCode,
                addressLine1 = addressLine1,
                addressLine2 = addressLine2,
                latitude = latitude,
                longitude = longitude,
                isDefault = isDefault
            )
        }
    }
}