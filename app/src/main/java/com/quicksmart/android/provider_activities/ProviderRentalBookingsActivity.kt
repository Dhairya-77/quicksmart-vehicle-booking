package com.quicksmart.android.provider_activities

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.Firebase
import com.google.firebase.app
import com.google.firebase.firestore.FirebaseFirestore
import com.quicksmart.android.R
import com.quicksmart.android.provider_activities.adapter_model.ProviderRentalBookingAdapter
import com.quicksmart.android.provider_activities.adapter_model.RentalRequestModel
import com.quicksmart.android.reusable_methods.getString
import com.quicksmart.android.reusable_methods.showConfirmDialog
import com.quicksmart.android.reusable_methods.showToast

class ProviderRentalBookingsActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance(Firebase.app, "quicksmart-db")
    private lateinit var adapter: ProviderRentalBookingAdapter
    private val bookingList = mutableListOf<RentalRequestModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_provider_rental_bookings)

        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        val recycler: RecyclerView = findViewById(R.id.recycler_rental_bookings)
        val emptyState: LinearLayout = findViewById(R.id.layout_empty)
        val progressBar: android.widget.ProgressBar = findViewById(R.id.progressBar)

        recycler.layoutManager = LinearLayoutManager(this)
        adapter = ProviderRentalBookingAdapter(bookingList) { item ->
            handleCompleteTrip(item, progressBar)
        }
        recycler.adapter = adapter

        fetchAcceptedBookings(emptyState, recycler, progressBar)
    }

    private fun fetchAcceptedBookings(emptyState: View, recycler: View, progressBar: View) {
        val ownerId = getString("userId")
        if (ownerId.isEmpty()) return

        progressBar.visibility = View.VISIBLE
        db.collection("bookings")
            .whereEqualTo("ownerId", ownerId)
            .whereEqualTo("bookingType", "rental")
            .whereEqualTo("status", "accepted")
            .addSnapshotListener { snapshot, e ->
                progressBar.visibility = View.GONE
                if (e != null) {
                    showToast("Error loading bookings: ${e.message}")
                    return@addSnapshotListener
                }

                bookingList.clear()
                snapshot?.documents?.forEach { doc ->
                    val model = doc.toObject(RentalRequestModel::class.java)?.copy(bookingId = doc.id)
                    if (model != null) bookingList.add(model)
                }

                if (bookingList.isEmpty()) {
                    recycler.visibility = View.GONE
                    emptyState.visibility = View.VISIBLE
                } else {
                    emptyState.visibility = View.GONE
                    recycler.visibility = View.VISIBLE
                    adapter.notifyDataSetChanged()
                }
            }
    }

    private fun handleCompleteTrip(item: RentalRequestModel, progressBar: View) {
        val tripSummary = "Are you sure you want to mark this trip as completed?\n\n" +
            "Vehicle: ${item.vehicleNo}\n" +
            "Consumer: ${item.consumerName}\n" +
            "Duration: ${item.tripDays}"

        showConfirmDialog(
            title = "Complete Trip",
            message = tripSummary
        ) { confirmed ->
            if (!confirmed) return@showConfirmDialog

            progressBar.visibility = View.VISIBLE
            db.collection("bookings").document(item.bookingId)
                .get()
                .addOnSuccessListener { doc ->
                    val paymentDone = doc.getBoolean("paymentDone") ?: false
                    val status = doc.getString("status") ?: ""

                    if (!paymentDone) {
                        progressBar.visibility = View.GONE
                        showToast("Cannot complete: payment not received yet.")
                        return@addOnSuccessListener
                    }

                    if (status == "completed") {
                        progressBar.visibility = View.GONE
                        showToast("Trip is already completed.")
                        return@addOnSuccessListener
                    }

                    db.collection("bookings").document(item.bookingId)
                        .update("status", "completed")
                        .addOnSuccessListener {
                            progressBar.visibility = View.GONE
                            showToast("Trip completed successfully!")
                        }
                        .addOnFailureListener { e ->
                            progressBar.visibility = View.GONE
                            showToast("Failed to complete trip: ${e.message}")
                        }
                }
                .addOnFailureListener { e ->
                    progressBar.visibility = View.GONE
                    showToast("Failed to verify payment: ${e.message}")
                }
        }
    }
}
