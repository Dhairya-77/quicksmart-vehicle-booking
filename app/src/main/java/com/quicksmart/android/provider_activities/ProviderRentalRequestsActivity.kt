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
import com.quicksmart.android.provider_activities.adapter_model.RentalRequestAdapter
import com.quicksmart.android.provider_activities.adapter_model.RentalRequestModel
import com.quicksmart.android.reusable_methods.getString
import com.quicksmart.android.reusable_methods.showToast

class ProviderRentalRequestsActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance(Firebase.app, "quicksmart-db")
    private lateinit var adapter: RentalRequestAdapter
    private val requestList = mutableListOf<RentalRequestModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_provider_rental_requests)

        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        val recycler:   RecyclerView = findViewById(R.id.recycler_rental_requests)
        val emptyState: LinearLayout = findViewById(R.id.layout_empty)
        val progressBar: android.widget.ProgressBar = findViewById(R.id.progressBar)

        recycler.layoutManager = LinearLayoutManager(this)
        adapter = RentalRequestAdapter(requestList) { item, isAccepted ->
            handleRequestAction(item, isAccepted, progressBar)
        }
        recycler.adapter = adapter

        fetchRentalRequests(emptyState, recycler, progressBar)
    }

    private fun fetchRentalRequests(emptyState: View, recycler: View, progressBar: View) {
        val ownerId = getString("userId")
        if (ownerId.isEmpty()) return

        progressBar.visibility = View.VISIBLE
        db.collection("bookings")
            .whereEqualTo("ownerId", ownerId)
            .whereEqualTo("bookingType", "rental")
            .whereEqualTo("status", "pending")
            .addSnapshotListener { snapshot, e ->
                progressBar.visibility = View.GONE
                if (e != null) {
                    showToast("Error loading requests: ${e.message}")
                    return@addSnapshotListener
                }

                requestList.clear()
                snapshot?.documents?.forEach { doc ->
                    val model = doc.toObject(RentalRequestModel::class.java)?.copy(bookingId = doc.id)
                    if (model != null) requestList.add(model)
                }

                if (requestList.isEmpty()) {
                    recycler.visibility   = View.GONE
                    emptyState.visibility = View.VISIBLE
                } else {
                    emptyState.visibility = View.GONE
                    recycler.visibility   = View.VISIBLE
                    adapter.notifyDataSetChanged()
                }
            }
    }

    private fun handleRequestAction(item: RentalRequestModel, isAccepted: Boolean, progressBar: View) {
        val newStatus = if (isAccepted) "accepted" else "rejected"
        
        progressBar.visibility = View.VISIBLE
        db.collection("bookings").document(item.bookingId)
            .update("status", newStatus)
            .addOnSuccessListener {
                progressBar.visibility = View.GONE
                showToast("Rental request ${newStatus}!")
                sendNotificationToConsumer(item, newStatus)
            }
            .addOnFailureListener { e ->
                progressBar.visibility = View.GONE
                showToast("Failed to update status: ${e.message}")
            }
    }

    private fun sendNotificationToConsumer(item: RentalRequestModel, status: String) {
        val notificationDoc = hashMapOf(
            "userId"    to item.consumerId,
            "title"     to "Rental Booking ${status.replaceFirstChar { it.uppercase() }}",
            "message"   to "Your rental booking for ${item.ownerName} (${item.vehicleNo}) has been ${status} by the provider.",
            "type"      to "status",
            "isRead"    to false,
            "createdAt" to com.google.firebase.Timestamp.now(),
            "expiryDate" to com.quicksmart.android.common_activities.NotificationsActivity.expiryTimestamp()
        )

        db.collection("notifications").add(notificationDoc)
    }
}
