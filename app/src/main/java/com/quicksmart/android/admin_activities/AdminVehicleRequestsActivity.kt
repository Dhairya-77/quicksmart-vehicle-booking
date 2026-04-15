package com.quicksmart.android.admin_activities

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.app
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.quicksmart.android.R
import com.quicksmart.android.admin_activities.adapter_model.AdminVehicleModel
import com.quicksmart.android.admin_activities.adapter_model.AdminVehicleRequestAdapter
import com.quicksmart.android.common_activities.NotificationsActivity
import com.quicksmart.android.reusable_methods.toFormattedDate

class AdminVehicleRequestsActivity : AppCompatActivity() {

    private val db      = FirebaseFirestore.getInstance(Firebase.app, "quicksmart-db")
    private val storage = FirebaseStorage.getInstance()

    private val items = mutableListOf<AdminVehicleModel>()
    private lateinit var adapter    : AdminVehicleRequestAdapter
    private lateinit var recycler   : RecyclerView
    private lateinit var emptyLayout: LinearLayout
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_vehicle_requests)

        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        recycler    = findViewById(R.id.requestsRecycler)
        emptyLayout = findViewById(R.id.emptyLayout)
        progressBar = findViewById(R.id.progressBar)

        recycler.layoutManager = LinearLayoutManager(this)

        adapter = AdminVehicleRequestAdapter(
            items     = items,
            onApprove = { vehicle, position -> approveVehicle(vehicle, position) },
            onReject  = { vehicle, position -> rejectVehicle(vehicle, position)  }
        )
        recycler.adapter = adapter

        loadPendingVehicles()
    }

    // ── Load pending vehicles ─────────────────────────────────────────────────

    private fun loadPendingVehicles() {
        progressBar.visibility = View.VISIBLE
        recycler.visibility    = View.GONE
        emptyLayout.visibility = View.GONE

        db.collection("vehicles")
            .whereEqualTo("status", "pending")
            .get()
            .addOnSuccessListener { snapshot ->
                progressBar.visibility = View.GONE
                items.clear()

                for (doc in snapshot.documents) {
                    items.add(
                        AdminVehicleModel(
                            docId         = doc.id,
                            vehicleType   = doc.getString("vehicleType")   ?: "",
                            vehicleNumber = doc.getString("vehicleNumber") ?: "",
                            ownerName     = doc.getString("ownerName")     ?: "",
                            ownerId       = doc.getString("ownerId")       ?: "",
                            purpose       = doc.getString("purpose")       ?: "",
                            seatCount     = doc.getLong("seatCount")?.toInt() ?: 0,
                            pricePerKm    = doc.getDouble("pricePerKm") ?: 0.0,
                            rcDocLink     = doc.getString("rcDocLink")     ?: "",
                            status        = doc.getString("status")        ?: "pending",
                            createdAt     = doc.getTimestamp("createdAt")?.toFormattedDate() ?: ""
                        )
                    )
                }

                adapter.notifyDataSetChanged()
                updateEmptyState()
            }
            .addOnFailureListener {
                progressBar.visibility = View.GONE
                updateEmptyState()
                Toast.makeText(this, "Failed to load vehicle requests", Toast.LENGTH_SHORT).show()
            }
    }

    // ── APPROVE ───────────────────────────────────────────────────────────────
    // 1. Update vehicle status → approved
    // 2. Send in-app notification to the provider

    private fun approveVehicle(vehicle: AdminVehicleModel, position: Int) {
        db.collection("vehicles").document(vehicle.docId)
            .update("status", "approved")
            .addOnSuccessListener {
                adapter.removeAt(position)
                updateEmptyState()
                Toast.makeText(this, "${vehicle.vehicleNumber} approved", Toast.LENGTH_SHORT).show()

                // Notify the provider
                if (vehicle.ownerId.isNotEmpty()) {
                    storeVehicleNotification(
                        providerId    = vehicle.ownerId,
                        vehicleNumber = vehicle.vehicleNumber,
                        approved      = true
                    )
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to approve vehicle", Toast.LENGTH_SHORT).show()
            }
    }

    // ── REJECT ────────────────────────────────────────────────────────────────
    // 1. Delete vehicle Firestore document
    // 2. Delete RC PDF from Firebase Storage (vehicle_rc/{ownerId}/{vehicleNo}.pdf)

    private fun rejectVehicle(vehicle: AdminVehicleModel, position: Int) {
        // Step 1: delete Firestore document
        db.collection("vehicles").document(vehicle.docId)
            .delete()
            .addOnSuccessListener {
                adapter.removeAt(position)
                updateEmptyState()
                Toast.makeText(this, "${vehicle.vehicleNumber} rejected & data removed", Toast.LENGTH_SHORT).show()

                // Notify the provider
                if (vehicle.ownerId.isNotEmpty()) {
                    storeVehicleNotification(
                        providerId    = vehicle.ownerId,
                        vehicleNumber = vehicle.vehicleNumber,
                        approved      = false
                    )
                }

                // Step 2: delete RC PDF from Storage
                if (vehicle.ownerId.isNotEmpty() && vehicle.vehicleNumber.isNotEmpty()) {
                    deleteRcFromStorage(vehicle.ownerId, vehicle.vehicleNumber)
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to reject vehicle", Toast.LENGTH_SHORT).show()
            }
    }

    // ── Delete RC PDF from Firebase Storage ───────────────────────────────────
    // Path: vehicle_rc/{ownerId}/{vehicleNumber}.pdf

    private fun deleteRcFromStorage(ownerId: String, vehicleNumber: String) {
        val rcRef = storage.reference
            .child("vehicle_rc/$ownerId/${vehicleNumber}.pdf")

        rcRef.delete()
            .addOnSuccessListener {
                Log.d("STORAGE", "RC deleted: vehicle_rc/$ownerId/${vehicleNumber}.pdf")
            }
            .addOnFailureListener { e ->
                // Non-fatal: file may have already been deleted or path mismatch
                Log.e("STORAGE", "RC delete failed: ${e.message}")
            }
    }

    // ── Store vehicle approval / rejection notification ────────────────────────

    private fun storeVehicleNotification(
        providerId    : String,
        vehicleNumber : String,
        approved      : Boolean
    ) {
        val title   = if (approved) "Vehicle Approved" else "Vehicle Rejected"
        val message = if (approved)
            "Your vehicle $vehicleNumber has been approved! You can now start accepting rides and rentals."
        else
            "Your vehicle $vehicleNumber was not approved. Please re-submit with correct documents."

        val notification = hashMapOf(
            "userId"     to providerId,
            "title"      to title,
            "message"    to message,
            "type"       to if (approved) "vehicle_approved" else "vehicle_rejected",
            "isRead"     to false,
            "createdAt"  to Timestamp.now(),
            "expiryDate" to NotificationsActivity.expiryTimestamp()
        )

        db.collection("notifications").add(notification)
            .addOnSuccessListener {
                Log.d("NOTIFY", "Vehicle notification sent to provider $providerId")
            }
            .addOnFailureListener { e ->
                Log.e("NOTIFY", "Failed to store vehicle notification: ${e.message}")
            }
    }

    // ── UI helpers ────────────────────────────────────────────────────────────

    private fun updateEmptyState() {
        if (items.isEmpty()) {
            recycler.visibility    = View.GONE
            emptyLayout.visibility = View.VISIBLE
        } else {
            recycler.visibility    = View.VISIBLE
            emptyLayout.visibility = View.GONE
        }
    }
}
