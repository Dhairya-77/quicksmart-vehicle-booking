package com.quicksmart.android.reusable_methods

import com.quicksmart.android.BuildConfig
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

object CashfreeHelper {

    private val client = OkHttpClient()

    // Sandbox endpoint. Switch to https://api.cashfree.com/pg/orders for production.
    private const val BASE_URL = "https://sandbox.cashfree.com/pg/orders"
    private const val API_VERSION = "2023-08-01"

    data class OrderResult(
        val orderId: String = "",
        val paymentSessionId: String = "",
        val error: String? = null
    )

    /**
     * Creates a Cashfree order on the sandbox server and returns the
     * [OrderResult] with [orderId] and [paymentSessionId] required by the SDK.
     *
     * Must be called from a background thread (use OkHttp async or coroutine).
     *
     * @param consumerUid  Firebase UID used as customer_id
     * @param consumerName Full name of the consumer
     * @param consumerEmail Consumer email (use fallback if unavailable)
     * @param consumerPhone 10-digit phone number
     * @param amountStr    Amount as a string, e.g. "120" (numeric only, no ₹ symbol)
     * @param bookingId    Firestore booking document ID – used as the unique order_id
     * @param callback     Called on the main thread with the [OrderResult]
     */
    fun createOrder(
        consumerUid:   String,
        consumerName:  String,
        consumerEmail: String,
        consumerPhone: String,
        amountStr:    String,
        bookingId:    String,
        callback:    (OrderResult) -> Unit
    ) {
        val appKey    = BuildConfig.CASHFREE_APP_KEY
        val secretKey = BuildConfig.CASHFREE_SECRET_KEY

        // Strip any non-numeric characters from the amount (e.g. "₹120" → "120")
        val cleanAmount = amountStr.replace(Regex("[^0-9.]"), "").ifEmpty { "1" }

        val body = JSONObject().apply {
            // Append timestamp to ensure uniqueness on retries (Cashfree orders cannot have duplicate IDs)
            val uniqueOrderId = "QS_${bookingId.take(12)}_${System.currentTimeMillis() / 1000}"
            put("order_id",     uniqueOrderId)
            // Send as a Double to avoid unmarshalling errors on Cashfree's side
            put("order_amount", cleanAmount.toDoubleOrNull() ?: 1.0)
            put("order_currency", "INR")
            put("customer_details", JSONObject().apply {
                put("customer_id",    consumerUid.take(50))
                put("customer_name",  consumerName.ifEmpty { "Consumer" })
                put("customer_email", consumerEmail.ifEmpty { "customer@quicksmart.in" })
                put("customer_phone", consumerPhone.filter(Char::isDigit).takeLast(10)
                    .padStart(10, '9'))
            })
            put("order_meta", JSONObject().apply {
                put("return_url", "https://quicksmart.in/payment?order_id={order_id}")
            })
        }.toString()

        val request = Request.Builder()
            .url(BASE_URL)
            .post(body.toRequestBody("application/json".toMediaType()))
            .addHeader("x-api-version",  API_VERSION)
            .addHeader("x-client-id",    appKey)
            .addHeader("x-client-secret", secretKey)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(OrderResult(error = "Network error: ${e.message}"))
            }

            override fun onResponse(call: Call, response: Response) {
                val bodyStr = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    callback(OrderResult(error = "Server error ${response.code}: $bodyStr"))
                    return
                }
                try {
                    val json = JSONObject(bodyStr)
                    callback(
                        OrderResult(
                            orderId          = json.getString("order_id"),
                            paymentSessionId = json.getString("payment_session_id")
                        )
                    )
                } catch (e: Exception) {
                    callback(OrderResult(error = "Parse error: ${e.message}"))
                }
            }
        })
    }
}
