package org.example.domain.models.identity

import com.google.gson.annotations.SerializedName
import kotlinx.datetime.LocalDateTime
import org.example.domain.validations.NotBlank
import org.example.utils.now
import org.example.utils.shortUUID
import java.io.Serializable
import java.math.BigDecimal
import java.time.OffsetDateTime

data class Address(
    val id: String = shortUUID(),

    @SerializedName(value = "user_id")
    val userId: String,

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
) : Serializable