package com.quicksmart.android.provider_activities

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.Firebase
import com.google.firebase.app
import com.google.firebase.firestore.FirebaseFirestore
import com.quicksmart.android.R
import com.quicksmart.android.provider_activities.adapter_model.ProviderVehicleAdapter
import com.quicksmart.android.provider_activities.adapter_model.ProviderVehicleModel
import com.quicksmart.android.reusable_methods.getString

class VehicleListActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance(Firebase.app, "quicksmart-db")

    private val items = mutableListOf<ProviderVehicleModel>()
    private lateinit var adapter    : ProviderVehicleAdapter
    private lateinit var recycler   : RecyclerView
    private lateinit var emptyState : LinearLayout
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vehicle_list)

        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        recycler    = findViewById(R.id.recycler_vehicle_list)
        emptyState  = findViewById(R.id.layout_empty)
        progressBar = findViewById(R.id.progress_bar)

        recycler.layoutManager = LinearLayoutManager(this)
        
        adapter = ProviderVehicleAdapter(
            items    = items,
            onDelete = { vehicle, position -> showDeleteConfirmation(vehicle, position) },
            onUpdatePurpose = { vehicle, newPurpose -> updateVehiclePurpose(vehicle, newPurpose) },
            onUpdatePrice = { vehicle, newPrice -> updateVehiclePrice(vehicle, newPrice) }
        )
        recycler.adapter = adapter

        loadMyVehicles()
    }

    private fun loadMyVehicles() {
        val userId = getString("userId")
        if (userId.isEmpty()) return

        progressBar.visibility = View.VISIBLE
        recycler.visibility    = View.GONE
        emptyState.visibility  = View.GONE

        db.collection("vehicles")
            .whereEqualTo("ownerId", userId)
            .whereEqualTo("status", "approved")
            .get()
            .addOnSuccessListener { snapshot ->
                progressBar.visibility = View.GONE
                items.clear()

                for (doc in snapshot.documents) {
                    items.add(
                        ProviderVehicleModel(
                            docId         = doc.id,
                            vehicleType   = doc.getString("vehicleType")   ?: "",
                            vehicleNumber = doc.getString("vehicleNumber") ?: "",
                            purpose       = doc.getString("purpose")       ?: "",
                            seatCount     = doc.getLong("seatCount")?.toInt() ?: 0,
                            pricePerKm    = doc.getDouble("pricePerKm") ?: 0.0,
                            status        = doc.getString("status")        ?: "pending"
                        )
                    )
                }

                adapter.notifyDataSetChanged()
                updateUIState()
            }
            .addOnFailureListener { e ->
                progressBar.visibility = View.GONE
                updateUIState()
                Toast.makeText(this, "Failed to load vehicles: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateVehiclePurpose(vehicle: ProviderVehicleModel, newPurpose: String) {
        db.collection("vehicles").document(vehicle.docId)
            .update("purpose", newPurpose)
            .addOnSuccessListener {
                Toast.makeText(this, "Purpose updated to $newPurpose", Toast.LENGTH_SHORT).show()
                loadMyVehicles() // Reload to sync UI (e.g. show/hide pricing field)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to update purpose: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateVehiclePrice(vehicle: ProviderVehicleModel, newPrice: Double) {
        db.collection("vehicles").document(vehicle.docId)
            .update("pricePerKm", newPrice)
            .addOnSuccessListener {
                Toast.makeText(this, "Price updated to ₹$newPrice / KM", Toast.LENGTH_SHORT).show()
                loadMyVehicles()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to update price: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showDeleteConfirmation(vehicle: ProviderVehicleModel, position: Int) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Delete Vehicle")
            .setMessage("Are you sure you want to delete ${vehicle.vehicleNumber}?\nThis will remove it from the system permanently.")
            .setPositiveButton("Delete") { dialog, _ ->
                dialog.dismiss()
                Toast.makeText(this, "Deleting ${vehicle.vehicleNumber}...", Toast.LENGTH_SHORT).show()
                deleteVehicle(vehicle, position)
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
                Toast.makeText(this, "Cancelled", Toast.LENGTH_SHORT).show()
             }
            .show()
    }

    private fun deleteVehicle(vehicle: ProviderVehicleModel, position: Int) {
        db.collection("vehicles").document(vehicle.docId)
            .delete()
            .addOnSuccessListener {
                adapter.removeAt(position)
                updateUIState()
                Toast.makeText(this, "Vehicle removed", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Delete failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateUIState() {
        if (items.isEmpty()) {
            recycler.visibility   = View.GONE
            emptyState.visibility = View.VISIBLE
        } else {
            recycler.visibility   = View.VISIBLE
            emptyState.visibility = View.GONE
        }
    }
}
