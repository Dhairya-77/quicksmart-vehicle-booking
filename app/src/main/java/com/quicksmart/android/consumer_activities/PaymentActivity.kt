package com.quicksmart.android.consumer_activities

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.cashfree.pg.api.CFPaymentGatewayService
import com.cashfree.pg.core.api.CFSession
import com.cashfree.pg.core.api.callback.CFCheckoutResponseCallback
import com.cashfree.pg.core.api.exception.CFException
import com.cashfree.pg.core.api.utils.CFErrorResponse
import com.cashfree.pg.core.api.webcheckout.CFWebCheckoutPayment
import com.cashfree.pg.core.api.webcheckout.CFWebCheckoutTheme
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.app
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.quicksmart.android.R
import com.quicksmart.android.reusable_methods.CashfreeHelper
import com.quicksmart.android.reusable_methods.showToast
import java.util.Calendar

/**
 * Launched from BookingsFragment when the consumer taps "Pay Now" or "Pay".
 * It creates a Cashfree order, then hands off to the Cashfree Web Checkout screen.
 *
 * On success  → marks `paymentDone = true` on the booking doc (status is NOT changed;
 *               only the provider can complete the ride via their "Complete Ride" button)
 *             → writes a document to the `transactions` Firestore collection
 *
 * Required Intent extras:
 *  - EXTRA_BOOKING_ID   : Firestore booking document ID
 *  - EXTRA_BOOKING_TYPE : "ride" | "rent"
 *  - EXTRA_AMOUNT       : fare string (e.g. "₹240" or "240")
 *  - EXTRA_PROVIDER_ID  : provider UID (for transaction record)
 */
class PaymentActivity : AppCompatActivity(), CFCheckoutResponseCallback {

    companion object {
        const val EXTRA_BOOKING_ID   = "booking_id"
        const val EXTRA_BOOKING_TYPE = "booking_type"
        const val EXTRA_AMOUNT       = "amount"
        const val EXTRA_PROVIDER_ID  = "provider_id"
    }

    private val db   = FirebaseFirestore.getInstance(Firebase.app, "quicksmart-db")
    private val auth = FirebaseAuth.getInstance()

    private lateinit var statusText : TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var retryBtn   : Button

    private var bookingId   = ""
    private var bookingType = ""
    private var amount      = ""
    private var providerId  = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment)

        bookingId   = intent.getStringExtra(EXTRA_BOOKING_ID)   ?: ""
        bookingType = intent.getStringExtra(EXTRA_BOOKING_TYPE) ?: ""
        amount      = intent.getStringExtra(EXTRA_AMOUNT)       ?: ""
        providerId  = intent.getStringExtra(EXTRA_PROVIDER_ID)  ?: ""

        statusText  = findViewById(R.id.paymentStatus)
        progressBar = findViewById(R.id.paymentProgress)
        retryBtn    = findViewById(R.id.btnRetryPayment)

        retryBtn.setOnClickListener { startPayment() }

        // Register SDK callback — must be done inside onCreate
        try {
            CFPaymentGatewayService.getInstance().setCheckoutCallback(this)
        } catch (e: CFException) {
            Log.e("PaymentActivity", "CFException: ${e.message}")
        }

        startPayment()
    }

    // ── Create Cashfree order and launch SDK checkout ─────────────────────
    private fun startPayment() {
        val user = auth.currentUser ?: run {
            showToast("Please login to pay")
            finish()
            return
        }

        progressBar.visibility = View.VISIBLE
        statusText.text        = "Creating payment order…"
        retryBtn.visibility    = View.GONE

        db.collection("users").document(user.uid).get()
            .addOnSuccessListener { doc ->
                val name  = "${doc.getString("firstName") ?: ""} ${doc.getString("lastName") ?: ""}".trim()
                val email = doc.getString("email")  ?: ""
                val phone = doc.getString("mobile") ?: ""

                CashfreeHelper.createOrder(
                    consumerUid   = user.uid,
                    consumerName  = name,
                    consumerEmail = email,
                    consumerPhone = phone,
                    amountStr     = amount,
                    bookingId     = bookingId
                ) { result ->
                    runOnUiThread {
                        progressBar.visibility = View.GONE
                        if (result.error != null) {
                            statusText.text     = "Failed to create order:\n${result.error}"
                            retryBtn.visibility = View.VISIBLE
                        } else {
                            launchCheckout(result.orderId, result.paymentSessionId)
                        }
                    }
                }
            }
            .addOnFailureListener {
                progressBar.visibility = View.GONE
                statusText.text        = "Failed to load profile: ${it.message}"
                retryBtn.visibility    = View.VISIBLE
            }
    }

    // ── Open Cashfree Web Checkout UI ─────────────────────────────────────
    private fun launchCheckout(orderId: String, sessionId: String) {
        try {
            val cfSession = CFSession.CFSessionBuilder()
                .setEnvironment(CFSession.Environment.SANDBOX)
                .setPaymentSessionID(sessionId)
                .setOrderId(orderId)
                .build()

            val cfTheme = CFWebCheckoutTheme.CFWebCheckoutThemeBuilder()
                .setNavigationBarBackgroundColor("#6A3FD3")
                .setNavigationBarTextColor("#FFFFFF")
                .build()

            val payment = CFWebCheckoutPayment.CFWebCheckoutPaymentBuilder()
                .setSession(cfSession)
                .setCFWebCheckoutUITheme(cfTheme)
                .build()

            statusText.text = "Opening payment page…"
            CFPaymentGatewayService.getInstance().doPayment(this, payment)

        } catch (e: CFException) {
            Log.e("PaymentActivity", "Checkout error: ${e.message}")
            statusText.text     = "Checkout error: ${e.message}"
            retryBtn.visibility = View.VISIBLE
        }
    }

    // ── Cashfree callbacks ────────────────────────────────────────────────
    override fun onPaymentVerify(orderId: String) {
        Log.d("PaymentActivity", "Payment verified: $orderId")

        val consumerId = auth.currentUser?.uid ?: ""

        // 1. Parse numeric amount (extract first numeric value, ignoring text like " (est.)" or "/km")
        val cleanAmount = amount.replace(",", "")
        val match = Regex("[0-9]+(?:\\.[0-9]+)?").find(cleanAmount)
        val numericAmount = match?.value?.toDoubleOrNull() ?: 0.0

        val typeLabel = if (bookingType.equals("rent", ignoreCase = true)) "Rental Payment" else "Ride Payment"

        // 2. Persist transaction
        val txn = hashMapOf(
            "bookingId"       to bookingId,
            "bookingType"     to bookingType,
            "consumerId"      to consumerId,
            "providerId"      to providerId,
            "amount"          to numericAmount,
            "title"           to typeLabel,
            "cashfreeOrderId" to orderId,
            "paidAt"          to Timestamp.now()
        )
        db.collection("transactions").add(txn)
            .addOnSuccessListener { docRef ->
                Log.d("PaymentActivity", "Transaction saved: ${docRef.id}")

                // 3. Update booking record with payment status and transaction reference
                db.collection("bookings").document(bookingId).update(
                    mapOf(
                        "paymentDone"   to true,
                        "transactionId" to docRef.id
                    )
                ).addOnFailureListener { Log.e("PaymentActivity", "Booking update failed: ${it.message}") }
            }
            .addOnFailureListener { Log.e("PaymentActivity", "Transaction save failed: ${it.message}") }

        showToast("Payment successful! Provider will mark your ride as Complete.")
        setResult(RESULT_OK)
        finish()
    }

    override fun onPaymentFailure(error: CFErrorResponse, orderId: String) {
        Log.e("PaymentActivity", "Payment failed [$orderId]: ${error.message}")
        runOnUiThread {
            statusText.text     = "Payment failed: ${error.message}"
            retryBtn.visibility = View.VISIBLE
        }
    }
}
