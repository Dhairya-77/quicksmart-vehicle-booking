package com.quicksmart.android.provider_activities

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.tasks.Tasks
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.Firebase
import com.google.firebase.app
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.quicksmart.android.R
import com.quicksmart.android.consumer_activities.adapter_model.BookingHistoryAdapter
import com.quicksmart.android.consumer_activities.adapter_model.BookingHistoryModel
import com.quicksmart.android.reusable_methods.getTimestampSafe
import com.quicksmart.android.reusable_methods.showToast
import java.text.SimpleDateFormat
import java.util.Locale

class ProviderBookingHistoryActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance(Firebase.app, "quicksmart-db")
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_provider_booking_history)

        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        val recycler: RecyclerView = findViewById(R.id.recycler_view)
        val emptyState: LinearLayout = findViewById(R.id.layout_empty)
        val progressBar: android.widget.ProgressBar = findViewById(R.id.progressBar)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        recycler.layoutManager = LinearLayoutManager(this)

        val providerId = auth.currentUser?.uid ?: run {
            emptyState.visibility = View.VISIBLE
            return
        }

        progressBar.visibility = View.VISIBLE

        val rideTask = db.collection("bookings")
            .whereEqualTo("providerId", providerId)
            .whereEqualTo("bookingType", "ride")
            .whereIn("status", listOf("completed", "cancelled", "rejected"))
            .get()

        val rentalTask = db.collection("bookings")
            .whereEqualTo("ownerId", providerId)
            .whereEqualTo("bookingType", "rental")
            .whereIn("status", listOf("completed", "cancelled", "rejected"))
            .get()

        Tasks.whenAll(rideTask, rentalTask)
            .addOnSuccessListener {
                progressBar.visibility = View.GONE
                val allDocs = buildList {
                    addAll(rideTask.result?.documents ?: emptyList())
                    addAll(rentalTask.result?.documents ?: emptyList())
                }
                val items = allDocs
                    .map { mapDocToHistoryItem(it) }
                    .sortedByDescending { it.second }
                    .map { it.first }

                if (items.isEmpty()) {
                    recycler.visibility = View.GONE
                    emptyState.visibility = View.VISIBLE
                } else {
                    recycler.adapter = BookingHistoryAdapter(items)
                    emptyState.visibility = View.GONE
                }
            }
            .addOnFailureListener {
                progressBar.visibility = View.GONE
                showToast("Failed to load booking history: ${it.message}")
                emptyState.visibility = View.VISIBLE
            }
    }

    private fun mapDocToHistoryItem(doc: DocumentSnapshot): Pair<BookingHistoryModel, Long> {
        val type = doc.getString("bookingType") ?: ""
        val isRental = type.equals("rental", ignoreCase = true)
        val status = doc.getString("status") ?: ""
        val bookingDate = doc.getTimestampSafe("bookingDate")
        val sortMillis  = bookingDate?.toDate()?.time ?: 0L
        val dateStr = bookingDate?.toDate()?.let {
            SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(it)
        } ?: "-"

        val consumerName = doc.getString("consumerName") ?: "—"
        val providerName = doc.getString("providerName") ?: "—"
        val vehicleNo = doc.getString("vehicleNo") ?: "—"

        val (rideType, from, to, fare, duration) = if (isRental) {
            listOf(
                doc.getString("vehicleType") ?: "Rental",
                doc.getString("pickupLocation") ?: "-",
                doc.getString("dropoffLocation") ?: "-",
                doc.getString("amount") ?: "-",
                doc.getString("tripDays") ?: "-"
            )
        } else {
            listOf(
                (doc.getString("vehicleType") ?: "Ride").replaceFirstChar { it.uppercase() } + " Ride",
                doc.getString("fromAddress") ?: "-",
                doc.getString("toAddress") ?: "-",
                doc.getString("fare") ?: "-",
                "-"
            )
        }

        return BookingHistoryModel(
            rideType = rideType,
            from = from,
            to = to,
            amount = fare,
            duration = duration,
            status = status.replaceFirstChar { it.uppercase() },
            date = dateStr,
            isRental = isRental,
            consumerName = consumerName,
            providerName = providerName,
            vehicleNo = vehicleNo
        ) to sortMillis
    }
}
