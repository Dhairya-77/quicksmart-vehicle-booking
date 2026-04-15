package com.quicksmart.android.consumer_activities.fragments

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.quicksmart.android.R
import com.quicksmart.android.common_activities.LoginActivity
import com.quicksmart.android.consumer_activities.BookingHistoryActivity
import com.quicksmart.android.consumer_activities.ConsumerPolicyActivity
import com.quicksmart.android.consumer_activities.ProfileEditActivity
import com.quicksmart.android.consumer_activities.TransactionHistoryActivity
import com.quicksmart.android.reusable_methods.clearAll
import com.quicksmart.android.reusable_methods.getString
import com.quicksmart.android.reusable_methods.showConfirmDialog
import com.quicksmart.android.reusable_methods.showToast
import de.hdodenhof.circleimageview.CircleImageView

class AccountCenterFragment : Fragment(R.layout.fragment_consumer_account_center) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ── Navigation ────────────────────────────────────────────────────────────
        view.findViewById<CardView>(R.id.card_profile_edit).setOnClickListener {
            startActivity(Intent(requireContext(), ProfileEditActivity::class.java))
        }

        view.findViewById<CardView>(R.id.card_booking_history).setOnClickListener {
            startActivity(Intent(requireContext(), BookingHistoryActivity::class.java))
        }

        view.findViewById<CardView>(R.id.card_transaction_history).setOnClickListener {
            startActivity(Intent(requireContext(), TransactionHistoryActivity::class.java))
        }

        view.findViewById<CardView>(R.id.card_term_and_con).setOnClickListener {
            startActivity(Intent(requireContext(), ConsumerPolicyActivity::class.java))
        }

        // ── Logout ────────────────────────────────────────────────────────────────
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
    }

    private fun updateUI() {
        val view = view ?: return
        
        // Populate user info from SharedPreferences
        val firstName  = requireContext().getString("firstName", "")
        val lastName   = requireContext().getString("lastName", "")
        val profileImg = requireContext().getString("profileImg", "")

        val fullName = buildString {
            if (firstName.isNotEmpty()) append(firstName)
            if (lastName.isNotEmpty()) { if (isNotEmpty()) append(" "); append(lastName) }
            if (isEmpty()) append("User")
        }

        view.findViewById<TextView>(R.id.name).text = fullName

        // Avatar via Glide
        val imgAvatar = view.findViewById<CircleImageView>(R.id.img_avatar)
        Glide.with(this)
            .load(profileImg.ifEmpty { null })
            .placeholder(R.drawable.ic_person)
            .error(R.drawable.ic_person)
            .into(imgAvatar)
    }
}
