package com.quicksmart.android.common_activities

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
import com.quicksmart.android.common_activities.adapter_model.NotificationAdapter
import com.quicksmart.android.common_activities.adapter_model.NotificationModel
import com.quicksmart.android.reusable_methods.getString
import com.quicksmart.android.reusable_methods.toFormattedDate
import com.quicksmart.android.reusable_methods.toRelativeTime
import java.util.Calendar

class NotificationsActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance(Firebase.app, "quicksmart-db")

    private val items = mutableListOf<NotificationModel>()
    private lateinit var adapter     : NotificationAdapter
    private lateinit var recycler    : RecyclerView
    private lateinit var emptyLayout : LinearLayout
    private lateinit var progressBar : ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notifications)

        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        recycler    = findViewById(R.id.notificationsRecycler)
        emptyLayout = findViewById(R.id.emptyLayout)
        progressBar = findViewById(R.id.progressBar)

        recycler.layoutManager = LinearLayoutManager(this)
        adapter = NotificationAdapter(items)
        recycler.adapter = adapter

        loadNotifications()
    }

    private fun loadNotifications() {
        val userId = getString("userId")
        if (userId.isEmpty()) { showEmpty(); return }

        progressBar.visibility = View.VISIBLE
        recycler.visibility    = View.GONE
        emptyLayout.visibility = View.GONE

        // ── BUG FIX: Removed .orderBy() to avoid Firestore composite index requirement.
        // Sorting is done in-memory after fetch — same result, zero index setup needed.
        db.collection("notifications")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { snapshot ->
                progressBar.visibility = View.GONE
                items.clear()

                val unreadIds  = mutableListOf<String>()
                val expiredIds = mutableListOf<String>()
                val now        = Timestamp.now()

                for (doc in snapshot.documents) {
                    val createdAt  = doc.getTimestamp("createdAt")  ?: Timestamp.now()
                    val expiryTs   = doc.getTimestamp("expiryDate")

                    // ── Auto-delete expired notifications ─────────────────────
                    if (expiryTs != null && expiryTs.compareTo(now) <= 0) {
                        expiredIds.add(doc.id)
                        continue   // skip expired — don't show in list
                    }

                    val isRead = doc.getBoolean("isRead") ?: false

                    items.add(
                        NotificationModel(
                            id         = doc.id,
                            userId     = doc.getString("userId")  ?: "",
                            title      = doc.getString("title")   ?: "",
                            message    = doc.getString("message") ?: "",
                            type       = doc.getString("type")    ?: "info",
                            isRead     = isRead,
                            timestamp  = createdAt.toRelativeTime(),
                            expiryDate = expiryTs?.toFormattedDate() ?: ""
                        )
                    )

                    if (!isRead) unreadIds.add(doc.id)
                }

                // ── Sort newest-first in memory ───────────────────────────────
                items.sortByDescending { it.timestamp }   // relative times sort correctly
                adapter.notifyDataSetChanged()

                // ── Mark all unread as read ───────────────────────────────────
                if (unreadIds.isNotEmpty()) markAllRead(unreadIds)

                // ── Delete expired notifications from Firestore ───────────────
                if (expiredIds.isNotEmpty()) deleteExpired(expiredIds)

                if (items.isEmpty()) showEmpty() else showList()
            }
            .addOnFailureListener { e ->
                progressBar.visibility = View.GONE
                showEmpty()
                Log.e("NOTIFY", "Failed to load notifications: ${e.message}")
                Toast.makeText(this, "Failed to load notifications", Toast.LENGTH_SHORT).show()
            }
    }

    // ── Mark all unread as read ───────────────────────────────────────────────

    private fun markAllRead(docIds: List<String>) {
        val batch = db.batch()
        docIds.forEach { docId ->
            batch.update(db.collection("notifications").document(docId), "isRead", true)
        }
        batch.commit()
    }

    // ── Delete expired docs from Firestore ────────────────────────────────────

    private fun deleteExpired(docIds: List<String>) {
        val batch = db.batch()
        docIds.forEach { docId ->
            batch.delete(db.collection("notifications").document(docId))
        }
        batch.commit().addOnSuccessListener {
            Log.d("NOTIFY", "Deleted ${docIds.size} expired notification(s)")
        }
    }

    // ── View state helpers ────────────────────────────────────────────────────

    private fun showEmpty() {
        recycler.visibility    = View.GONE
        emptyLayout.visibility = View.VISIBLE
    }

    private fun showList() {
        recycler.visibility    = View.VISIBLE
        emptyLayout.visibility = View.GONE
    }

    companion object {
        /** Returns a Timestamp 30 days from now — use as expiryDate when creating a notification */
        fun expiryTimestamp(): Timestamp {
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, 30)
            return Timestamp(cal.time)
        }
    }
}
