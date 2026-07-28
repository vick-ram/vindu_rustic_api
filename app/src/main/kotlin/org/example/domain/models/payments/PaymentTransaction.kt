package org.example.domain.models.payments

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.example.domain.validations.OneOf
import org.example.utils.Ulid
import java.math.BigDecimal
import java.time.OffsetDateTime

@Serializable
data class PaymentTransaction(
    val id: String = Ulid.generate(),

    @SerialName("payment_id")
    val paymentId: String,

    @SerialName("provider_transaction_id")
    val providerTransactionId: String? = null,

    @OneOf("charge", "authorize", "capture", "refund", "void")
    @SerialName("transaction_type")
    val transactionType: String,

    @Contextual
    val amount: BigDecimal,
    val currency: String = "kes",

    @OneOf("success", "failure", "pending")
    val status: String,

    @Contextual
    @SerialName("provider_response")
    val providerResponse: Map<String, Any>? = null,

    @SerialName("error_message")
    val errorMessage: String? = null,

    @Contextual
    @SerialName("created_at")
    val createdAt: OffsetDateTime = OffsetDateTime.now()
)
