package org.example.domain.models.identity

import com.google.gson.annotations.SerializedName
import io.ktor.http.Parameters
import org.example.data.db.config.Ulid
import org.example.domain.validations.NotBlank
import java.io.Serializable
import java.math.BigDecimal
import java.time.OffsetDateTime

data class Address(
    val id: String = Ulid.generate(),

    @SerializedName(value = "user_id")
    val userId: String? = null,

    val label: String? = null,

    @field:NotBlank(message = "Name cannot be blank")
    @SerializedName("recipient_name")
    val recipientName: String,

    @field:NotBlank(message = "Phone cannot be blank")
    @SerializedName("phone_number")
    val phoneNumber: String,

    @SerializedName(value = "country_code")
    val countryCode: String = "+254",

    val country: String = "kenya",
    val city: String? = null,
    val state: String? = null,

    @SerializedName(value = "postal_code")
    val postalCode: String? = null,

    @SerializedName(value = "address_line1")
    val addressLine1: String,

    @SerializedName(value = "address_line2")
    val addressLine2: String? = null,

    val latitude: BigDecimal? = null,
    val longitude: BigDecimal? = null,

    @SerializedName(value = "is_default")
    val isDefault: Boolean = false,

    @SerializedName(value = "created_at")
    val createdAt: OffsetDateTime = OffsetDateTime.now()
) : Serializable {
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