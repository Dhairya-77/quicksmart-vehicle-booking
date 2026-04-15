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
import com.quicksmart.android.consumer_activities.adapter_model.TransactionAdapter
import com.quicksmart.android.consumer_activities.adapter_model.TransactionModel
import com.quicksmart.android.reusable_methods.showToast
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Displays the consumer's payment transaction history fetched from the `transactions`
 * Firestore collection (populated by PaymentActivity on successful Cashfree payment).
 */
class TransactionHistoryActivity : AppCompatActivity() {

    private val db   = FirebaseFirestore.getInstance(Firebase.app, "quicksmart-db")
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transaction_history)

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
        db.collection("transactions")
            .whereEqualTo("consumerId", consumerId)
            .get()
            .addOnSuccessListener { snapshot ->
                progressBar.visibility = View.GONE
                val items = snapshot.documents.mapNotNull { doc ->
                    val title   = doc.getString("title") ?: "Payment"
                    val type    = doc.getString("bookingType") ?: ""
                    val amount  = doc.getDouble("amount") ?: 0.0
                    val paidAt  = doc.getTimestamp("paidAt")?.toDate()
                    val dateStr = paidAt?.let {
                        SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(it)
                    } ?: ""
                    TransactionModel(
                        title    = title,
                        subtitle = type.replaceFirstChar { it.uppercase() },
                        amount   = "-₹%.0f".format(amount),
                        date     = dateStr,
                        isCredit = false
                    ) to (paidAt?.time ?: 0L)
                }

                val sortedItems = items.sortedByDescending { it.second }.map { it.first }

                if (sortedItems.isEmpty()) {
                    recycler.visibility   = View.GONE
                    emptyState.visibility = View.VISIBLE
                } else {
                    recycler.adapter      = TransactionAdapter(sortedItems)
                    emptyState.visibility = View.GONE
                }
            }
            .addOnFailureListener {
                progressBar.visibility = View.GONE
                showToast("Failed to load transactions: ${it.message}")
                emptyState.visibility = View.VISIBLE
            }
    }
}
