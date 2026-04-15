package com.quicksmart.android.admin_activities.fragments

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.app
import com.google.firebase.firestore.FirebaseFirestore
import com.quicksmart.android.R
import com.quicksmart.android.admin_activities.AdminProviderListActivity
import com.quicksmart.android.admin_activities.AdminProviderRequestsActivity
import com.quicksmart.android.admin_activities.AdminVehicleListActivity
import com.quicksmart.android.admin_activities.AdminVehicleRequestsActivity
import java.util.Calendar

class HomeFragment : Fragment(R.layout.fragment_admin_home) {

    private val db = FirebaseFirestore.getInstance(Firebase.app, "quicksmart-db")

    private lateinit var homeProgress: ProgressBar
    private lateinit var monthlyEarnings: TextView
    private lateinit var instantRideCount: TextView
    private lateinit var rentBookingCount: TextView
    private lateinit var providerRequestBadge: TextView
    private lateinit var vehicleRequestBadge: TextView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        homeProgress = view.findViewById(R.id.homeProgress)
        monthlyEarnings = view.findViewById(R.id.monthlyEarnings)
        instantRideCount = view.findViewById(R.id.instantRideCount)
        rentBookingCount = view.findViewById(R.id.rentBookingCount)
        providerRequestBadge = view.findViewById(R.id.providerRequestBadge)
        vehicleRequestBadge = view.findViewById(R.id.vehicleRequestBadge)

        view.findViewById<CardView>(R.id.cardProviderRequest).setOnClickListener {
            startActivity(Intent(requireContext(), AdminProviderRequestsActivity::class.java))
        }
        view.findViewById<CardView>(R.id.cardProviderList).setOnClickListener {
            startActivity(Intent(requireContext(), AdminProviderListActivity::class.java))
        }
        view.findViewById<CardView>(R.id.cardVehicleRequest).setOnClickListener {
            startActivity(Intent(requireContext(), AdminVehicleRequestsActivity::class.java))
        }
        view.findViewById<CardView>(R.id.cardAdminVehicleList).setOnClickListener {
            startActivity(Intent(requireContext(), AdminVehicleListActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        if (::homeProgress.isInitialized) fetchDashboard()
    }

    private fun fetchDashboard() {
        homeProgress.visibility = View.VISIBLE

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

        var pending = 4
        fun done() {
            pending -= 1
            if (pending == 0) homeProgress.visibility = View.GONE
        }

        db.collection("users")
            .whereEqualTo("role", "provider")
            .whereEqualTo("status", "pending")
            .get()
            .addOnSuccessListener { snap ->
                val count = snap.size()
                providerRequestBadge.text = if (count > 99) "99+" else count.toString()
                providerRequestBadge.visibility = if (count > 0) View.VISIBLE else View.GONE
                done()
            }
            .addOnFailureListener { done() }

        db.collection("vehicles")
            .whereEqualTo("status", "pending")
            .get()
            .addOnSuccessListener { snap ->
                val count = snap.size()
                vehicleRequestBadge.text = if (count > 99) "99+" else count.toString()
                vehicleRequestBadge.visibility = if (count > 0) View.VISIBLE else View.GONE
                done()
            }
            .addOnFailureListener { done() }

        db.collection("bookings")
            .get()
            .addOnSuccessListener { snap ->
                var ridesThisMonth = 0
                var rentalsThisMonth = 0
                
                snap.documents.forEach { doc ->
                    val type = doc.getString("bookingType") ?: ""
                    val ts = doc.getTimestamp("createdAt") ?: doc.getTimestamp("bookingDate")
                    
                    if (ts != null && ts >= monthStart && ts < nextMonthStart) {
                        if (type == "ride") ridesThisMonth++
                        else if (type == "rental" || type == "rent") rentalsThisMonth++
                    }
                }
                
                instantRideCount.text = ridesThisMonth.toString()
                rentBookingCount.text = rentalsThisMonth.toString()
                done()
            }
            .addOnFailureListener { done() }

        db.collection("transactions")
            .get()
            .addOnSuccessListener { snap ->
                var adminCutTotal = 0.0
                snap.documents.forEach { doc ->
                    val paidAt = doc.getTimestamp("paidAt")
                    if (paidAt != null && paidAt >= monthStart && paidAt < nextMonthStart) {
                        val txnAmount = doc.getDouble("amount") ?: 0.0
                        adminCutTotal += (txnAmount * 0.10)
                    }
                }
                monthlyEarnings.text = "₹ %.2f".format(adminCutTotal)
                done()
            }
            .addOnFailureListener {
                monthlyEarnings.text = "₹ 0.00"
                done()
            }
    }
}
