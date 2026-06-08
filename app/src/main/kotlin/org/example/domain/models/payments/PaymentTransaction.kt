package org.example.domain.models.payments

import com.google.gson.annotations.SerializedName
import org.example.data.db.config.Ulid
import java.io.Serializable
import java.math.BigDecimal
import java.time.OffsetDateTime

data class PaymentTransaction(
    val id: String = Ulid.generate(),

    @SerializedName("payment_id")
    val paymentId: String,

    @SerializedName("provider_transaction_id")
    val providerTransactionId: String? = null,

    @SerializedName("transaction_type")
    val transactionType: String,

    val amount: BigDecimal,
    val currency: String = "kes",
    val status: String,

    @SerializedName("provider_response")
    val providerResponse: Map<String, Any>? = null,

    @SerializedName("error_message")
    val errorMessage: String? = null,

    @SerializedName("created_at")
    val createdAt: OffsetDateTime = OffsetDateTime.now()
): Serializable
