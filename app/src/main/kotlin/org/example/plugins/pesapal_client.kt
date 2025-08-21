package org.example.plugins

import com.google.gson.annotations.SerializedName
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class PesapalClient(
    private val httpClient: HttpClient,
    private val pesapalBaseUrl: String,
    private val consumerKey: String,
    private val consumerSecret: String,
    private val autoRefreshToken: Boolean = true
) {
    private var token: String? = null
    private var tokenExpiry: Long = 0
    private val tokenMutex = Mutex()

    suspend fun ensureAuthenticated() {
        if (autoRefreshToken && (token == null || System.currentTimeMillis() > tokenExpiry)) {
            refreshToken()
        }
    }

    private suspend fun refreshToken(): String = tokenMutex.withLock {
        // Check again in case another coroutine already refreshed the token
        if (token != null && System.currentTimeMillis() < tokenExpiry) {
            return token!!
        }

        val response = httpClient.post("${pesapalBaseUrl}/Auth/RequestToken") {
            contentType(ContentType.Application.Json)
            setBody(AuthRequest(consumerKey, consumerSecret))
        }

        when (response.status) {
            HttpStatusCode.OK -> {
                val authResponse = response.body<AuthResponse>()
                token = authResponse.token
                // Assuming token expires in 1 hour (adjust according to Pesapal's actual expiry)
                tokenExpiry = System.currentTimeMillis() + 3_600_000
                authResponse.token
            }

            else -> throw PesapalException(
                "Failed to authenticate with Pesapal: ${response.status} - ${response.bodyAsText()}"
            )
        }
    }

    suspend fun registerIpn(ipnUrl: String, notificationType: String): IPNResponse {
        ensureAuthenticated()
        return httpClient.post("${pesapalBaseUrl}/URLSetup/RegisterIPN") {
            contentType(ContentType.Application.Json)
            bearerAuth(token!!)
            setBody(IPNRequest(ipnUrl, notificationType))
        }.handleResponse<IPNResponse>()
    }

    suspend fun getRegisteredIpn(): List<IPNResponse> {
        ensureAuthenticated()
        return httpClient.get("${pesapalBaseUrl}/URLSetup/GetIpnList") {
            bearerAuth(token!!)
        }.handleResponse<List<IPNResponse>>()
    }

    suspend fun submitOrder(
        orderRequest: OrderRequest
    ): OrderResponse {
        ensureAuthenticated()
        return httpClient.post("${pesapalBaseUrl}/Transactions/SubmitOrderRequest") {
            contentType(ContentType.Application.Json)
            bearerAuth(token!!)
            setBody(orderRequest)
        }.handleResponse<OrderResponse>()
    }

    suspend fun getTransactionStatus(orderTrackingId: String): OrderTransactionResponse {
        ensureAuthenticated()
        return httpClient.get("${pesapalBaseUrl}/Transactions/GetTransactionStatus") {
            contentType(ContentType.Application.Json)
            bearerAuth(token!!)
            parameter("orderTrackingId", orderTrackingId)
        }.handleResponse<OrderTransactionResponse>()
    }

    suspend fun requestRefund(refundRequest: RefundRequest): RefundResponse {
        ensureAuthenticated()
        return httpClient.post("${pesapalBaseUrl}/Transactions/RefundRequest") {
            contentType(ContentType.Application.Json)
            bearerAuth(token!!)
            setBody(refundRequest)
        }.handleResponse<RefundResponse>()
    }

    suspend fun cancelOrder(orderCancellationRequest: OrderCancellationRequest): OrderCancellationResponse {
        ensureAuthenticated()
        return httpClient.post("${pesapalBaseUrl}/Transactions/CancelOrder") {
            contentType(ContentType.Application.Json)
            bearerAuth(token!!)
            setBody(orderCancellationRequest)
        }.handleResponse<OrderCancellationResponse>()
    }

    private suspend inline fun <reified T> HttpResponse.handleResponse(): T {
        return when (status) {
            HttpStatusCode.OK -> body()
            HttpStatusCode.Unauthorized -> {
                if (autoRefreshToken) {
                    refreshToken()
                    throw PesapalAuthException("Authentication expired, please retry with new token")
                } else {
                    throw PesapalAuthException("Authentication failed")
                }
            }
            else -> throw PesapalException("Pesapal API request failed: $status - ${bodyAsText()}")
        }
    }
}


data class AuthRequest(
    @SerializedName("consumer_key") val consumerKey: String,
    @SerializedName("consumer_secret") val consumerSecret: String
)

data class AuthResponse(
    val token: String,
    val expiryDate: String,
    val error: ErrorResponse? = null,
    val status: String,
    val message: String
)

data class IPNRequest(
    val url: String,
    @SerializedName("ipn_notification_type") val ipnNotificationType: String
)

data class IPNResponse(
    val url: String,
    @SerializedName("created_date") val createdDate: String,
    @SerializedName("ipn_id") val ipnId: String,
    @SerializedName("notification_type") val notificationType: Int,
    @SerializedName("ipn_notification_type_description") val ipnNotificationTypeDescription: String,
    @SerializedName("ipn_status") val ipnStatus: Int,
    @SerializedName("ipn_status_description") val ipnStatusDescription: String,
    val error: ErrorResponse? = null,
    val status: String,
)

data class OrderRequest(
    val id: String,
    val currency: String,
    val amount: Float,
    val description: String,
    @SerializedName("redirect_mode") val redirectMode: String?,
    @SerializedName("callback_url") val callbackUrl: String,
    @SerializedName("cancellation_url") val cancellationUrl: String,
    @SerializedName("notification_id") val notificationId: String,
    val branch: String?,
    val billingAddress: BillingAddress,
)

data class BillingAddress(
    @SerializedName("phone_number") val phoneNumber: String,
    @SerializedName("email_address") val emailAddress: String,
    @SerializedName("country_code") val countryCode: String,
    @SerializedName("first_name") val firstName: String,
    @SerializedName("last_name") val lastName: String,
    @SerializedName("line_1") val lineOne: String?,
    @SerializedName("line_2") val lineTwo: String?,
    val city: String?,
    val state: String?,
    @SerializedName("postal_code") val postalCode: String?,
    @SerializedName("zip_code") val zipCode: String?,
)

data class OrderResponse(
    @SerializedName("order_tracking_id") val orderTrackingId: String,
    @SerializedName("merchant_reference") val merchantReference: String,
    @SerializedName("redirect_url") val redirectUrl: String,
    val error: ErrorResponse? = null,
    val message: String,
)

data class OrderTransactionResponse(
    @SerializedName("payment_method") val paymentMethod: String,
    val amount: Float,
    @SerializedName("created_date") val createdDate: String,
    @SerializedName("confirmation_code") val confirmationCode: String,
    @SerializedName("payment_status_description") val paymentStatusDescription: String,
    val description: String,
    val message: String,
    @SerializedName("payment_account") val paymentAccount: String,
    @SerializedName("call_back_url") val callbackUrl: String,
    @SerializedName("status_code") val statusCode: Int,
    @SerializedName("merchant_reference") val merchantReference: String,
    @SerializedName("payment_status_code") val paymentStatusCode: String,
    val currency: String,
    val error: ErrorResponse?,
    val status: String
)

data class RefundRequest(
    val confirmationCode: String,
    val amount: String,
    val username: String,
    val remarks: String,
)

data class RefundResponse(
    val status: String,
    val message: String,
)

data class OrderCancellationRequest(
    val orderTrackingId: String
)

data class OrderCancellationResponse(
    val status: String,
    val message: String,
)

data class ErrorResponse(
    val type: String,
    val code: String,
    val message: String
)