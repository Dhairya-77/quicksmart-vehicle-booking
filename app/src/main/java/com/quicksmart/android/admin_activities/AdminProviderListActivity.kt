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
import com.quicksmart.android.admin_activities.adapter_model.AdminProviderModel
import com.quicksmart.android.admin_activities.adapter_model.AdminProviderListAdapter
import com.quicksmart.android.reusable_methods.toFormattedDate

class AdminProviderListActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance(Firebase.app, "quicksmart-db")

    private lateinit var recycler   : RecyclerView
    private lateinit var emptyLayout: LinearLayout
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_provider_list)

        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        recycler    = findViewById(R.id.providersRecycler)
        emptyLayout = findViewById(R.id.emptyLayout)
        progressBar = findViewById(R.id.progressBar)

        recycler.layoutManager = LinearLayoutManager(this)

        loadApprovedProviders()
    }

    // ── Firestore: fetch users with role=provider and status=approved ───────────
    private fun loadApprovedProviders() {
        progressBar.visibility = View.VISIBLE
        recycler.visibility    = View.GONE
        emptyLayout.visibility = View.GONE

        db.collection("users")
            .whereEqualTo("role", "provider")
            .whereEqualTo("status", "approved")
            .get()
            .addOnSuccessListener { snapshot ->
                progressBar.visibility = View.GONE

                val items = mutableListOf<AdminProviderModel>()
                for (doc in snapshot.documents) {
                    items.add(
                        AdminProviderModel(
                            userId        = doc.id,
                            firstName     = doc.getString("firstName")     ?: "",
                            lastName      = doc.getString("lastName")      ?: "",
                            mobile        = doc.getString("mobile")        ?: "",
                            email         = doc.getString("email")         ?: "",
                            status        = doc.getString("status")        ?: "approved",
                            profileImg = doc.getString("profileImgUrl") ?: "",
                            aadhaarUrl    = doc.getString("aadhaarUrl")    ?: "",
                            licenceUrl    = doc.getString("licenceUrl")    ?: "",
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
                    recycler.adapter = AdminProviderListAdapter(items)
                }
            }
            .addOnFailureListener {
                progressBar.visibility = View.GONE
                emptyLayout.visibility = View.VISIBLE
                Toast.makeText(this, "Failed to load providers", Toast.LENGTH_SHORT).show()
            }
    }
}
