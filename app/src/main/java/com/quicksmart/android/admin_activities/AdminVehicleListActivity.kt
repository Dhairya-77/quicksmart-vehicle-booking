package com.quicksmart.android.admin_activities

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
import com.quicksmart.android.admin_activities.adapter_model.AdminVehicleModel
import com.quicksmart.android.admin_activities.adapter_model.AdminVehicleListAdapter
import com.quicksmart.android.reusable_methods.toFormattedDate

class AdminVehicleListActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance(Firebase.app, "quicksmart-db")

    private lateinit var recycler   : RecyclerView
    private lateinit var emptyLayout: LinearLayout
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_vehicle_list)

        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        recycler    = findViewById(R.id.vehiclesRecycler)
        emptyLayout = findViewById(R.id.emptyLayout)
        progressBar = findViewById(R.id.progressBar)

        recycler.layoutManager = LinearLayoutManager(this)

        loadApprovedVehicles()
    }

    private fun loadApprovedVehicles() {
        progressBar.visibility = View.VISIBLE
        recycler.visibility    = View.GONE
        emptyLayout.visibility = View.GONE

        db.collection("vehicles")
            .get()
            .addOnSuccessListener { snapshot ->
                progressBar.visibility = View.GONE

                val items = mutableListOf<AdminVehicleModel>()
                for (doc in snapshot.documents) {
                    items.add(
                        AdminVehicleModel(
                            docId         = doc.id,
                            vehicleType   = doc.getString("vehicleType")   ?: "",
                            vehicleNumber = doc.getString("vehicleNumber") ?: "",
                            ownerName     = doc.getString("ownerName")     ?: "",
                            purpose       = doc.getString("purpose")       ?: "",
                            seatCount     = doc.getLong("seatCount")?.toInt() ?: 0,
                            pricePerKm    = doc.getDouble("pricePerKm") ?: 0.0,
                            rcDocLink     = doc.getString("rcDocLink")     ?: "",
                            status        = doc.getString("status")        ?: "approved",
                            createdAt     = doc.getTimestamp("createdAt")?.toFormattedDate() ?: ""
                        )
                    )
                }

                if (items.isEmpty()) {
                    recycler.visibility    = View.GONE
                    emptyLayout.visibility = View.VISIBLE
                } else {
                    recycler.visibility    = View.VISIBLE
                    emptyLayout.visibility = View.GONE
                    recycler.adapter = AdminVehicleListAdapter(items)
                }
            }
            .addOnFailureListener {
                progressBar.visibility = View.GONE
                emptyLayout.visibility = View.VISIBLE
                Toast.makeText(this, "Failed to load vehicles", Toast.LENGTH_SHORT).show()
            }
    }
}
