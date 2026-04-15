package com.quicksmart.android.provider_activities.fragments

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.google.android.material.card.MaterialCardView
import com.google.firebase.Firebase
import com.google.firebase.app
import com.google.firebase.firestore.FirebaseFirestore
import com.quicksmart.android.R
import com.quicksmart.android.provider_activities.AddVehicleActivity
import com.quicksmart.android.provider_activities.ProviderMapActivity
import com.quicksmart.android.provider_activities.ProviderRentalBookingsActivity
import com.quicksmart.android.provider_activities.ProviderRentalRequestsActivity
import com.quicksmart.android.provider_activities.VehicleListActivity
import com.quicksmart.android.reusable_methods.getString

class ProviderHomeFragment : Fragment(R.layout.fragment_provider_home) {

    private val db = FirebaseFirestore.getInstance(Firebase.app, "quicksmart-db")
    private var rentalRequestBadge: TextView? = null
    private var rentalRequestTask: com.google.firebase.firestore.ListenerRegistration? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Bind Badge
        rentalRequestBadge = view.findViewById(R.id.rentalRequestBadge)

        // Greeting
        val firstName = requireContext().getString("firstName", "Provider")
        view.findViewById<TextView>(R.id.welcomeTitle)?.text = "Welcome back, $firstName"

        // Wire click listeners
        setupCardListeners(view)
    }

    override fun onStart() {
        super.onStart()
        // Always check status and start badge listener when home is shown/returned to
        checkProviderStatus()
        startRentalRequestBadgeListener()
    }

    override fun onStop() {
        super.onStop()
        rentalRequestTask?.remove()
        rentalRequestTask = null
    }

    // ── Rental Request Badge Listener ──────────────────────────────────────────

    private fun startRentalRequestBadgeListener() {
        val userId = requireContext().getString("userId")
        if (userId.isEmpty()) return

        rentalRequestTask?.remove() // Safety: remove existing listener before starting new one

        rentalRequestTask = db.collection("bookings")
            .whereEqualTo("ownerId", userId)
            .whereEqualTo("bookingType", "rental")
            .whereEqualTo("status", "pending")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener

                val count = snapshot.size()
                updateRentalRequestBadge(count)
            }
    }

    private fun updateRentalRequestBadge(count: Int) {
        rentalRequestBadge?.apply {
            if (count > 0) {
                text = if (count > 99) "99+" else count.toString()
                visibility = View.VISIBLE
            } else {
                visibility = View.GONE
            }
        }
    }

    // ── Card click listeners ──────────────────────────────────────────────────

    private fun setupCardListeners(view: View) {
        view.findViewById<MaterialCardView>(R.id.cardInstanceRide)?.setOnClickListener {
            startActivity(Intent(requireContext(), ProviderMapActivity::class.java))
        }
        view.findViewById<MaterialCardView>(R.id.cardRentalRequest)?.setOnClickListener {
            startActivity(Intent(requireContext(), ProviderRentalRequestsActivity::class.java))
        }
        view.findViewById<MaterialCardView>(R.id.cardRentalBookings)?.setOnClickListener {
            startActivity(Intent(requireContext(), ProviderRentalBookingsActivity::class.java))
        }
        view.findViewById<MaterialCardView>(R.id.cardAddVehicle)?.setOnClickListener {
            startActivity(Intent(requireContext(), AddVehicleActivity::class.java))
        }
        view.findViewById<MaterialCardView>(R.id.cardVehicleList)?.setOnClickListener {
            startActivity(Intent(requireContext(), VehicleListActivity::class.java))
        }
    }

    // ── Status check ──────────────────────────────────────────────────────────

    private fun checkProviderStatus() {
        val userId = requireContext().getString("userId")
        if (userId.isEmpty()) return

        db.collection("users").document(userId).get()
            .addOnSuccessListener { doc ->
                if (!isAdded) return@addOnSuccessListener
                when (doc.getString("status") ?: "pending") {
                    "approved" -> checkVehicleUploaded(userId)
                    else       -> showPendingApprovalDialog()
                }
            }
    }

    // ── Pending: non-closeable, OK exits the app ──────────────────────────────

    private fun showPendingApprovalDialog() {
        if (!isAdded || activity?.isFinishing == true) return

        AlertDialog.Builder(requireContext())
            .setTitle("Account Pending Approval")
            .setMessage(
                "Your account is currently under review by the admin.\n\n" +
                "You will be notified once your account is approved. " +
                "This typically takes up to 24 hours.\n\n" +
                "Please check back after approval."
            )
            .setCancelable(false)                       // back button disabled
            .setPositiveButton("OK") { _, _ ->
                // Close the entire app — provider cannot access until approved
                requireActivity().finishAffinity()
            }
            .show()
    }

    // ── Approved but no vehicle: closeable, OK → AddVehicleActivity ──────────

    private fun checkVehicleUploaded(userId: String) {
        db.collection("vehicles")
            .whereEqualTo("ownerId", userId)
            .limit(1)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!isAdded) return@addOnSuccessListener
                if (snapshot.isEmpty) {
                    showUploadVehicleDialog()
                }
            }
    }

    private fun showUploadVehicleDialog() {
        if (!isAdded || activity?.isFinishing == true) return

        AlertDialog.Builder(requireContext())
            .setTitle("Upload Your Vehicle")
            .setMessage(
                "Your account has been approved!\n\n" +
                "To start accepting instant ride requests or rental bookings, " +
                "you must first add at least one vehicle."
            )
            .setCancelable(true)                        // closeable
            .setPositiveButton("Add Vehicle") { _, _ ->
                startActivity(Intent(requireContext(), AddVehicleActivity::class.java))
            }
            .setNegativeButton("Later", null)
            .show()
    }
}
