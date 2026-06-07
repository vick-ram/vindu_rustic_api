package org.example.services

import com.pesapal.sdk.PesapalClient
import org.example.data.db.entities.UserEntity
import org.example.domain.models.payments.Payment
import org.example.domain.models.sales.PaymentStatus
import org.example.domain.repo.PaymentRepository

class PaymentService(private val paymentRepository: PaymentRepository, private val pesapalClient: PesapalClient) {
    suspend fun initiatePayment(payment: Payment): Payment {
        val savedPayment = paymentRepository.create(payment)

        val user = UserEntity[payment.userId]

//        val orderRequest = OrderRequest(
//            id = savedPayment.orderId,
//            currency = payment.currency,
//            amount = payment.amount.toFloat(),
//            description = "Payment for product",
//            billingAddress = BillingAddress(
//                phoneNumber = user.phone,
//                emailAddress = user.email,
//                countryCode = "",
//                firstName = firstName,
//                lastName = lastName,
//                lineOne = null,
//                lineTwo = null,
//                city = null,
//                state = null,
//                postalCode = null,
//                zipCode = null
//            )
//        )
//
//        try {
//            val pesapalResponse = pesapalClient.submitOrder(orderRequest)
//
//            val updatedPayment = savedPayment.copy(
//                transactionReference = pesapalResponse.orderTrackingId,
//            )
//
//            paymentRepository.update(updatedPayment.id, updatedPayment)
//        } catch ()
        return savedPayment
    }

    suspend fun getPayment(id: String): Payment? {
        return paymentRepository.read(id)
    }

    suspend fun getPayments(offset: Int, limit: Int, queryParams: Map<String, String>?): List<Payment> {
        return paymentRepository.readAll(offset, limit, queryParams)
    }

    suspend fun getPaymentByOrder(orderId: String): Payment? {
        return paymentRepository.findByOrderId(orderId)
    }

    suspend fun getPaymentByTransactionRef(transactionRef: String): Payment? {
        return paymentRepository.findByTransactionRef(transactionRef)
    }

    suspend fun updatePaymentStatus(id: String, status: PaymentStatus): Payment? {
        return paymentRepository.updateStatus(id, status)
    }

    suspend fun deletePayment(id: String): Boolean {
        return paymentRepository.delete(id)
    }
}