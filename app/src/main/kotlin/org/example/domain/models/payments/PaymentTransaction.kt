package org.example.domain.models.payments

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.example.data.db.config.Ulid
import org.example.utils.BigDecimalSerializer
import org.example.utils.MapStringAnySerializer
import org.example.utils.OffsetDateTimeSerializer
import java.math.BigDecimal
import java.time.OffsetDateTime

@Serializable
data class PaymentTransaction(
    val id: String = Ulid.generate(),

    @SerialName("payment_id")
    val paymentId: String,

    @SerialName("provider_transaction_id")
    val providerTransactionId: String? = null,

    @SerialName("transaction_type")
    val transactionType: String,

    @Serializable(with = BigDecimalSerializer::class)
    val amount: BigDecimal,
    val currency: String = "kes",
    val status: String,

    @Serializable(with = MapStringAnySerializer::class)
    @SerialName("provider_response")
    val providerResponse: Map<String, Any>? = null,

    @SerialName("error_message")
    val errorMessage: String? = null,

    @Serializable(with = OffsetDateTimeSerializer::class)
    @SerialName("created_at")
    val createdAt: OffsetDateTime = OffsetDateTime.now()
)
