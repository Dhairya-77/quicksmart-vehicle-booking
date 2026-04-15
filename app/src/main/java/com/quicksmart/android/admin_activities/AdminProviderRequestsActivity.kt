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
import com.quicksmart.android.R
import com.quicksmart.android.admin_activities.adapter_model.AdminProviderModel
import com.quicksmart.android.admin_activities.adapter_model.AdminProviderRequestAdapter
import com.quicksmart.android.common_activities.NotificationsActivity
import com.quicksmart.android.reusable_methods.toFormattedDate

class AdminProviderRequestsActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance(Firebase.app, "quicksmart-db")

    private val items = mutableListOf<AdminProviderModel>()
    private lateinit var adapter    : AdminProviderRequestAdapter
    private lateinit var recycler   : RecyclerView
    private lateinit var emptyLayout: LinearLayout
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_provider_requests)

        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        recycler    = findViewById(R.id.requestsRecycler)
        emptyLayout = findViewById(R.id.emptyLayout)
        progressBar = findViewById(R.id.progressBar)

        recycler.layoutManager = LinearLayoutManager(this)

        adapter = AdminProviderRequestAdapter(
            items     = items,
            onApprove = { provider, position -> approveProvider(provider, position) },
            onReject  = { provider, position -> rejectProvider(provider, position)  }
        )
        recycler.adapter = adapter

        loadPendingProviders()
    }

    // ── Load pending providers ────────────────────────────────────────────────

    private fun loadPendingProviders() {
        progressBar.visibility = View.VISIBLE
        recycler.visibility    = View.GONE
        emptyLayout.visibility = View.GONE

        db.collection("users")
            .whereEqualTo("role", "provider")
            .whereEqualTo("status", "pending")
            .get()
            .addOnSuccessListener { snapshot ->
                progressBar.visibility = View.GONE
                items.clear()

                for (doc in snapshot.documents) {
                    items.add(
                        AdminProviderModel(
                            userId     = doc.id,
                            firstName  = doc.getString("firstName")     ?: "",
                            lastName   = doc.getString("lastName")      ?: "",
                            mobile     = doc.getString("mobile")        ?: "",
                            email      = doc.getString("email")         ?: "",
                            status     = doc.getString("status")        ?: "pending",
                            profileImg = doc.getString("profileImgUrl") ?: "",
                            aadhaarUrl = doc.getString("aadhaarUrl")    ?: "",
                            licenceUrl = doc.getString("licenceUrl")    ?: "",
                            createdAt  = doc.getTimestamp("createdAt")?.toFormattedDate() ?: ""
                        )
                    )
                }

                adapter.notifyDataSetChanged()
                updateEmptyState()
            }
            .addOnFailureListener {
                progressBar.visibility = View.GONE
                updateEmptyState()
                Toast.makeText(this, "Failed to load provider requests", Toast.LENGTH_SHORT).show()
            }
    }

    // ── APPROVE ───────────────────────────────────────────────────────────────

    private fun approveProvider(provider: AdminProviderModel, position: Int) {
        db.collection("users").document(provider.userId)
            .update("status", "approved")
            .addOnSuccessListener {
                adapter.removeAt(position)
                updateEmptyState()
                val name = "${provider.firstName} ${provider.lastName}".trim()
                Toast.makeText(this, "$name approved", Toast.LENGTH_SHORT).show()

                // Write notification record to Firestore flat collection.
                // The provider app detects this via a real-time Firestore snapshot listener
                // and shows a local system notification automatically.
                //
                // NOTE: For push notifications when the app is fully closed,
                // implement a Firebase Cloud Function that listens for status == "approved"
                // and sends an FCM HTTP v1 message using the Firebase Admin SDK.
                storeApprovalNotification(provider.userId, name)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to approve provider", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * Writes an approval record to the flat `notifications` collection.
     * Fields: userId, title, message, type, isRead, createdAt
     *
     * The provider's ProviderMainActivity snapshot listener picks this up and
     * shows a system-tray notification without any FCM server call.
     */
    private fun storeApprovalNotification(providerId: String, providerName: String) {
        val notification = hashMapOf(
            "userId"     to providerId,
            "title"      to "Account Approved",
            "message"    to "Congratulations $providerName! Your provider account has been approved. " +
                           "Please upload at least one vehicle to start accepting rides and rentals.",
            "type"       to "approval",
            "isRead"     to false,
            "createdAt"  to Timestamp.now(),
            "expiryDate" to NotificationsActivity.expiryTimestamp()   // auto-delete after 30 days
        )
        db.collection("notifications").add(notification)
            .addOnSuccessListener {
                Log.d("NOTIFY", "Approval notification stored for $providerId")
            }
            .addOnFailureListener { e ->
                Log.e("NOTIFY", "Failed to store notification: ${e.message}")
            }
    }

    // ── REJECT: delete Firestore doc + queue Auth deletion ───────────────────

    private fun rejectProvider(provider: AdminProviderModel, position: Int) {
        val name = "${provider.firstName} ${provider.lastName}".trim()

        db.collection("users").document(provider.userId)
            .delete()
            .addOnSuccessListener {
                adapter.removeAt(position)
                updateEmptyState()
                Toast.makeText(this, "$name rejected and data deleted", Toast.LENGTH_SHORT).show()

                // Purge any notification records that exist for this provider
                deleteProviderNotifications(provider.userId)

                // Queue Auth account deletion (requires Cloud Function or Admin SDK server-side)
                markForAuthDeletion(provider.userId, provider.email)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to reject provider", Toast.LENGTH_SHORT).show()
            }
    }

    private fun deleteProviderNotifications(providerId: String) {
        db.collection("notifications")
            .whereEqualTo("userId", providerId)
            .get()
            .addOnSuccessListener { snapshot ->
                val batch = db.batch()
                snapshot.documents.forEach { batch.delete(it.reference) }
                batch.commit()
            }
    }

    /**
     * Queues Auth account deletion via a Firestore document.
     * Wire a Cloud Function to watch `auth_deletion_queue` and call
     * admin.auth().deleteUser(uid) to remove the Firebase Auth record.
     */
    private fun markForAuthDeletion(userId: String, email: String) {
        val record = hashMapOf(
            "userId"    to userId,
            "email"     to email,
            "reason"    to "provider_rejected",
            "createdAt" to Timestamp.now()
        )
        db.collection("auth_deletion_queue").document(userId).set(record)
            .addOnSuccessListener {
                Log.d("AUTH", "Provider $email queued for Auth deletion")
            }
    }

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
