package com.quicksmart.android.provider_activities.fragments

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.app
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.quicksmart.android.R
import com.quicksmart.android.common_activities.LoginActivity
import com.quicksmart.android.provider_activities.ProviderBookingHistoryActivity
import com.quicksmart.android.provider_activities.ProviderEarningsActivity
import com.quicksmart.android.provider_activities.ProviderPolicyActivity
import com.quicksmart.android.provider_activities.ProviderProfileEditActivity
import com.quicksmart.android.reusable_methods.clearAll
import com.quicksmart.android.reusable_methods.getString
import com.quicksmart.android.reusable_methods.showConfirmDialog
import com.quicksmart.android.reusable_methods.showToast
import de.hdodenhof.circleimageview.CircleImageView
import java.util.Calendar

class ProviderAccountCenterFragment : Fragment(R.layout.fragment_provider_account_center) {

    private val db   = FirebaseFirestore.getInstance(Firebase.app, "quicksmart-db")
    private val auth = FirebaseAuth.getInstance()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ── Menu Items ─────────────────────────────────────────────────────────────
        view.findViewById<CardView>(R.id.card_profile_edit).setOnClickListener {
            startActivity(Intent(requireContext(), ProviderProfileEditActivity::class.java))
        }

        view.findViewById<CardView>(R.id.card_daily_earnings).setOnClickListener {
            startActivity(Intent(requireContext(), ProviderEarningsActivity::class.java))
        }

        view.findViewById<CardView>(R.id.card_booking_history).setOnClickListener {
            startActivity(Intent(requireContext(), ProviderBookingHistoryActivity::class.java))
        }

        view.findViewById<CardView>(R.id.card_term_and_con).setOnClickListener {
            startActivity(Intent(requireContext(), ProviderPolicyActivity::class.java))
        }

        // ── Logout ─────────────────────────────────────────────────────────────────
        view.findViewById<CardView>(R.id.card_logout).setOnClickListener {
            requireContext().showConfirmDialog(
                title = "Logout",
                message = "Are you sure you want to logout?",
                positiveLabel = "Yes",
                negativeLabel = "No"
            ) { confirmed ->
                if (confirmed) {
                    requireContext().showToast("Logging out...")
                    requireContext().clearAll()
                    val intent = Intent(requireContext(), LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                } else {
                    requireContext().showToast("Logout cancelled")
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateUI()
        fetchMonthlyEarnings()
    }

    // ── Populate profile info from SharedPreferences ──────────────────────────
    private fun updateUI() {
        val v = view ?: return

        val firstName  = requireContext().getString("firstName", "")
        val lastName   = requireContext().getString("lastName", "")
        val profileImg = requireContext().getString("profileImg", "")

        val fullName = buildString {
            if (firstName.isNotEmpty()) append(firstName)
            if (lastName.isNotEmpty()) { if (isNotEmpty()) append(" "); append(lastName) }
            if (isEmpty()) append("Provider")
        }

        v.findViewById<TextView>(R.id.provider_name).text = fullName

        val imgAvatar = v.findViewById<CircleImageView>(R.id.img_avatar)
        Glide.with(this)
            .load(profileImg.ifEmpty { null })
            .placeholder(R.drawable.ic_person)
            .error(R.drawable.ic_person)
            .into(imgAvatar)
    }

    // ── Fetch current-month total earnings from `transactions` collection ─────
    private fun fetchMonthlyEarnings() {
        val v          = view ?: return
        val providerId = auth.currentUser?.uid ?: return

        // Calculate start and end of current month
        val startCal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val nextMonthCal = startCal.clone() as Calendar
        nextMonthCal.add(Calendar.MONTH, 1)

        val monthStart = Timestamp(startCal.time)
        val nextMonthStart = Timestamp(nextMonthCal.time)

        db.collection("transactions")
            .whereEqualTo("providerId", providerId)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!isAdded) return@addOnSuccessListener
                var total = 0.0
                snapshot.documents.forEach { doc ->
                    val paidAt = doc.getTimestamp("paidAt")
                    if (paidAt != null && paidAt >= monthStart && paidAt < nextMonthStart) {
                        total += doc.getDouble("amount") ?: 0.0
                    }
                }
                v.findViewById<TextView>(R.id.total_earnings).text = "₹ %.0f".format(total)
            }
            .addOnFailureListener { }
    }
}
