package com.quicksmart.android.consumer_activities

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.Firebase
import com.google.firebase.app
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.quicksmart.android.R
import com.quicksmart.android.consumer_activities.adapter_model.BookingHistoryAdapter
import com.quicksmart.android.consumer_activities.adapter_model.BookingHistoryModel
import com.quicksmart.android.reusable_methods.getTimestampSafe
import com.quicksmart.android.reusable_methods.showToast
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Shows the consumer's complete booking history (both ride and rental) from the
 * `bookings` Firestore collection, ordered by bookingDate descending.
 * Only completed or cancelled bookings are shown (active ones appear in BookingsFragment).
 */
class BookingHistoryActivity : AppCompatActivity() {

    private val db   = FirebaseFirestore.getInstance(Firebase.app, "quicksmart-db")
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_booking_history)

        val toolbar    : MaterialToolbar = findViewById(R.id.toolbar)
        val recycler   : RecyclerView   = findViewById(R.id.recycler_view)
        val emptyState : LinearLayout   = findViewById(R.id.layout_empty)
        val progressBar : android.widget.ProgressBar = findViewById(R.id.progressBar)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        recycler.layoutManager = LinearLayoutManager(this)

        val consumerId = auth.currentUser?.uid ?: run {
            emptyState.visibility = View.VISIBLE
            return
        }

        progressBar.visibility = View.VISIBLE
        db.collection("bookings")
            .whereEqualTo("consumerId", consumerId)
            .whereIn("status", listOf("completed", "cancelled", "rejected"))
            .get()
            .addOnSuccessListener { snapshot ->
                progressBar.visibility = View.GONE
                val items = snapshot.documents.mapNotNull { doc ->
                    val type     = doc.getString("bookingType") ?: ""
                    val isRental = type.equals("rental", ignoreCase = true)
                    val status   = doc.getString("status") ?: ""
                    val bookingDate = doc.getTimestampSafe("bookingDate")
                    val dateStr = bookingDate?.toDate()?.let {
                        SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(it)
                    } ?: "—"
                    val sortMillis = bookingDate?.toDate()?.time ?: 0L

                    val consumerName = doc.getString("consumerName") ?: "—"
                    val providerName = doc.getString("providerName") ?: "—"
                    val vehicleNo = doc.getString("vehicleNo") ?: "—"

                    val from: String
                    val to: String
                    val fare: String
                    val duration: String
                    val rideType: String

                    if (isRental) {
                        rideType = doc.getString("vehicleType") ?: "Rental"
                        from     = doc.getString("pickupLocation") ?: "—"
                        to       = doc.getString("dropoffLocation") ?: "—"
                        fare     = doc.getString("amount") ?: "—"
                        duration = doc.getString("tripDays") ?: "—"
                    } else {
                        rideType = (doc.getString("vehicleType") ?: "Ride")
                            .replaceFirstChar { it.uppercase() } + " Ride"
                        from     = doc.getString("fromAddress") ?: "—"
                        to       = doc.getString("toAddress") ?: "—"
                        fare     = doc.getString("fare") ?: "—"
                        duration = "—"
                    }

                    BookingHistoryModel(
                        rideType = rideType,
                        from     = from,
                        to       = to,
                        amount   = fare,
                        duration = duration,
                        status   = status.replaceFirstChar { it.uppercase() },
                        date     = dateStr,
                        isRental = isRental,
                        consumerName = consumerName,
                        providerName = providerName,
                        vehicleNo = vehicleNo
                    ) to sortMillis
                }
                // Sort newest first
                val sorted = items.sortedByDescending { it.second }.map { it.first }

                if (sorted.isEmpty()) {
                    recycler.visibility   = View.GONE
                    emptyState.visibility = View.VISIBLE
                } else {
                    recycler.adapter      = BookingHistoryAdapter(sorted)
                    emptyState.visibility = View.GONE
                }
            }
            .addOnFailureListener {
                progressBar.visibility = View.GONE
                showToast("Failed to load booking history: ${it.message}")
                emptyState.visibility = View.VISIBLE
            }
    }
}
