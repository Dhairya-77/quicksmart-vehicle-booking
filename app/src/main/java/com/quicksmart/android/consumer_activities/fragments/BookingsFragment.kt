package com.quicksmart.android.consumer_activities.fragments

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.quicksmart.android.reusable_methods.getTimestampSafe
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.app
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.quicksmart.android.R
import com.quicksmart.android.consumer_activities.adapter_model.RentBookingAdapter
import com.quicksmart.android.consumer_activities.adapter_model.RideBookingAdapter
import com.quicksmart.android.consumer_activities.adapter_model.RideBookingModel
import com.quicksmart.android.provider_activities.adapter_model.RentalRequestModel
import com.quicksmart.android.reusable_methods.showConfirmDialog
import com.quicksmart.android.reusable_methods.getString
import com.quicksmart.android.reusable_methods.showToast
import com.quicksmart.android.reusable_methods.DirectionsHelper

/**
 * BookingsFragment — shows the consumer's current and upcoming bookings.
 */
class BookingsFragment : Fragment(R.layout.fragment_consumer_bookings) {

    // ── Tab state ─────────────────────────────────────────────────────────
    private enum class ActiveTab { RIDE, RENT }
    private var activeTab = ActiveTab.RIDE

    private val db = FirebaseFirestore.getInstance(Firebase.app, "quicksmart-db")

    // ── Ride listener ─────────────────────────────────────────────────────
    private var rideListener: ListenerRegistration? = null

    // ── Views ─────────────────────────────────────────────────────────────
    private lateinit var tabRide          : TextView
    private lateinit var tabRent          : TextView
    private lateinit var rideBookingsList : RecyclerView
    private lateinit var rentBookingsList : RecyclerView
    private lateinit var emptyState       : LinearLayout
    private lateinit var emptyStateText   : TextView
    private lateinit var progressBar      : android.widget.ProgressBar

    // ── Adapters ──────────────────────────────────────────────────────────
    private lateinit var rideAdapter: RideBookingAdapter
    private lateinit var rentAdapter: RentBookingAdapter

    // ── Data ──────────────────────────────────────────────────────────────
    private val rideBookings      = mutableListOf<RideBookingModel>()
    private val rentBookingsModels = mutableListOf<RentalRequestModel>()

    // ─────────────────────────────────────────────────────────────────────
    // Lifecycle
    // ─────────────────────────────────────────────────────────────────────
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindViews(view)
        setupAdapters()
        setupTabs()
        showTab(ActiveTab.RIDE)
        startRideBookingsListener()
        fetchRentBookings()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        rideListener?.remove()
        rideListener = null
    }

    // ─────────────────────────────────────────────────────────────────────
    // View binding
    // ─────────────────────────────────────────────────────────────────────
    private fun bindViews(view: View) {
        tabRide          = view.findViewById(R.id.tabRide)
        tabRent          = view.findViewById(R.id.tabRent)
        rideBookingsList = view.findViewById(R.id.rideBookingsList)
        rentBookingsList = view.findViewById(R.id.rentBookingsList)
        emptyState       = view.findViewById(R.id.emptyState)
        emptyStateText   = view.findViewById(R.id.emptyStateText)
        progressBar      = view.findViewById(R.id.progressBar)
    }

    // ─────────────────────────────────────────────────────────────────────
    // Adapters
    // ─────────────────────────────────────────────────────────────────────
    private fun setupAdapters() {
        rideAdapter = RideBookingAdapter(
            bookings        = rideBookings,
            onCancel        = { booking, pos -> handleRideCancel(booking, pos) },
            onPayNow        = { booking -> handleRidePay(booking) },
            onTrackDistance = { booking -> handleTrackDistance(booking) }
        )
        rideBookingsList.layoutManager = LinearLayoutManager(requireContext())
        rideBookingsList.adapter       = rideAdapter

        rentAdapter = RentBookingAdapter(
            bookings = rentBookingsModels,
            onCancel = { booking -> handleRentCancel(booking) },
            onPay    = { booking -> handleRentPay(booking) }
        )
        rentBookingsList.layoutManager = LinearLayoutManager(requireContext())
        rentBookingsList.adapter       = rentAdapter
    }

    // ─────────────────────────────────────────────────────────────────────
    // Fetch Ride Bookings from Firestore (real-time)
    // ─────────────────────────────────────────────────────────────────────
    private fun startRideBookingsListener() {
        val consumerId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""
        if (consumerId.isEmpty()) {
            progressBar.visibility = View.GONE
            updateEmptyState(true, "Please login to see bookings")
            return
        }

        progressBar.visibility = View.VISIBLE

        rideListener = db.collection("bookings")
            .whereEqualTo("consumerId", consumerId)
            .whereEqualTo("bookingType", "ride")
            .addSnapshotListener { snapshot, e ->
                if (!isAdded) return@addSnapshotListener
                progressBar.visibility = View.GONE
                if (e != null) {
                    requireContext().showToast("Error loading ride bookings: ${e.message}")
                    return@addSnapshotListener
                }

                rideBookings.clear()
                snapshot?.documents?.forEach { doc ->
                    val status = doc.getString("status") ?: ""
                    // Only show pending and accepted (active) bookings
                    if (status == "pending" || status == "accepted" || status == "picked_up") {
                        val model = RideBookingModel(
                            bookingId        = doc.id,
                            consumerId       = doc.getString("consumerId") ?: "",
                            providerId       = doc.getString("providerId") ?: "",
                            riderName        = doc.getString("providerName") ?: "",
                            vehicleType      = doc.getString("vehicleType") ?: "",
                            vehicleNo        = doc.getString("vehicleNo") ?: "",
                            fromAddress      = doc.getString("fromAddress") ?: "",
                            fromLat          = doc.getDouble("fromLat") ?: 0.0,
                            fromLng          = doc.getDouble("fromLng") ?: 0.0,
                            toAddress        = doc.getString("toAddress") ?: "",
                            toLat            = doc.getDouble("toLat") ?: 0.0,
                            toLng            = doc.getDouble("toLng") ?: 0.0,
                            providerLat      = doc.getDouble("providerLat") ?: 0.0,
                            providerLng      = doc.getDouble("providerLng") ?: 0.0,
                            totalPersons     = (doc.getLong("totalPersons") ?: 1L).toInt(),
                            perPersonPrice   = (doc.getLong("perPersonPrice") ?: 0L).toInt(),
                            fare             = doc.getString("fare") ?: "",
                            status           = status,
                            bookingType      = "ride",
                            bookingDate      = doc.getTimestampSafe("bookingDate"),
                            paymentDone      = doc.getBoolean("paymentDone") ?: false,
                            transactionId    = doc.getString("transactionId") ?: ""
                        )
                        rideBookings.add(model)
                    }
                }

                rideAdapter.updateList(rideBookings)
                if (activeTab == ActiveTab.RIDE) {
                    updateEmptyState(rideBookings.isEmpty(), "No ride bookings yet")
                }
            }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Fetch Rentals from Firestore
    // ─────────────────────────────────────────────────────────────────────
    private fun fetchRentBookings() {
        val consumerId = requireContext().getString("userId")
        if (consumerId.isEmpty()) return

        progressBar.visibility = View.VISIBLE
        db.collection("bookings")
            .whereEqualTo("consumerId", consumerId)
            .whereEqualTo("bookingType", "rental")
            .addSnapshotListener { snapshot, e ->
                if (!isAdded) return@addSnapshotListener
                progressBar.visibility = View.GONE
                if (e != null) {
                    requireContext().showToast("Error loading bookings: ${e.message}")
                    return@addSnapshotListener
                }

                rentBookingsModels.clear()
                snapshot?.documents?.forEach { doc ->
                    val model = doc.toObject(RentalRequestModel::class.java)
                        ?.copy(
                            bookingId   = doc.id,
                            paymentDone = doc.getBoolean("paymentDone") ?: false
                        )
                    if (model != null && (model.status == "pending" || model.status == "accepted")) {
                        rentBookingsModels.add(model)
                    }
                }
                rentAdapter.updateList(rentBookingsModels)

                if (activeTab == ActiveTab.RENT) {
                    updateEmptyState(rentBookingsModels.isEmpty(), "No rent bookings yet")
                }
            }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Tab logic
    // ─────────────────────────────────────────────────────────────────────
    private fun setupTabs() {
        tabRide.setOnClickListener { showTab(ActiveTab.RIDE) }
        tabRent.setOnClickListener { showTab(ActiveTab.RENT) }
    }

    private fun showTab(tab: ActiveTab) {
        activeTab = tab
        val ctx = requireContext()
        when (tab) {
            ActiveTab.RIDE -> {
                tabRide.setBackgroundColor(ContextCompat.getColor(ctx, R.color.darkPurple))
                tabRide.setTextColor(ContextCompat.getColor(ctx, R.color.white))
                tabRent.setBackgroundColor(ContextCompat.getColor(ctx, R.color.lightGray))
                tabRent.setTextColor(ContextCompat.getColor(ctx, R.color.darkPurple))
                rideBookingsList.visibility = View.VISIBLE
                rentBookingsList.visibility = View.GONE
                updateEmptyState(rideBookings.isEmpty(), "No ride bookings yet")
            }
            ActiveTab.RENT -> {
                tabRent.setBackgroundColor(ContextCompat.getColor(ctx, R.color.darkPurple))
                tabRent.setTextColor(ContextCompat.getColor(ctx, R.color.white))
                tabRide.setBackgroundColor(ContextCompat.getColor(ctx, R.color.lightGray))
                tabRide.setTextColor(ContextCompat.getColor(ctx, R.color.darkPurple))
                rentBookingsList.visibility = View.VISIBLE
                rideBookingsList.visibility = View.GONE
                updateEmptyState(rentBookingsModels.isEmpty(), "No rent bookings yet")
            }
        }
    }

    private fun updateEmptyState(isEmpty: Boolean, message: String) {
        if (isEmpty) {
            emptyState.visibility    = View.VISIBLE
            emptyStateText.text      = message
            rideBookingsList.visibility = View.GONE
            rentBookingsList.visibility = View.GONE
        } else {
            emptyState.visibility    = View.GONE
            if (activeTab == ActiveTab.RIDE) {
                rideBookingsList.visibility = View.VISIBLE
                rentBookingsList.visibility = View.GONE
            } else {
                rideBookingsList.visibility = View.GONE
                rentBookingsList.visibility = View.VISIBLE
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Action Handlers
    // ─────────────────────────────────────────────────────────────────────
    private fun handleRideCancel(booking: RideBookingModel, pos: Int) {
        requireContext().showConfirmDialog(
            title = "Cancel Booking",
            message = "Are you sure you want to cancel this ride booking?"
        ) { confirmed ->
            if (!confirmed) return@showConfirmDialog
            db.collection("bookings").document(booking.bookingId)
                .update("status", "cancelled")
                .addOnSuccessListener {
                    requireContext().showToast("Booking cancelled.")

                    // Notify provider
                    val notification = hashMapOf(
                        "userId"     to booking.providerId,
                        "title"      to "Ride Cancelled",
                        "message"    to "A consumer has cancelled their ride booking.",
                        "type"       to "ride_cancelled",
                        "isRead"     to false,
                        "createdAt"  to com.google.firebase.Timestamp.now(),
                        "expiryDate" to com.quicksmart.android.common_activities.NotificationsActivity.expiryTimestamp()
                    )
                    db.collection("notifications").add(notification)

                    // Restore seat count in RTDB
                    val rtdb = com.google.firebase.database.FirebaseDatabase.getInstance()
                    rtdb.getReference("live_rides/${booking.providerId}").get()
                        .addOnSuccessListener { snap ->
                            val occupied  = (snap.child("seatsOccupied").getValue(Int::class.java) ?: 0) - booking.totalPersons
                            val total     = snap.child("totalSeats").getValue(Int::class.java) ?: 0
                            val available = total - occupied.coerceAtLeast(0)
                            rtdb.getReference("live_rides/${booking.providerId}").updateChildren(
                                mapOf("seatsOccupied" to occupied.coerceAtLeast(0), "seatsAvailable" to available)
                            )
                            // Remove booking reference node
                            rtdb.getReference("live_rides/${booking.providerId}/bookings/${booking.bookingId}").removeValue()
                        }
                }
                .addOnFailureListener { requireContext().showToast("Failed to cancel: ${it.message}") }
        }
    }

    private fun handleRidePay(booking: RideBookingModel) {
        val intent = android.content.Intent(requireContext(), com.quicksmart.android.consumer_activities.PaymentActivity::class.java).apply {
            putExtra(com.quicksmart.android.consumer_activities.PaymentActivity.EXTRA_BOOKING_ID,   booking.bookingId)
            putExtra(com.quicksmart.android.consumer_activities.PaymentActivity.EXTRA_BOOKING_TYPE, "ride")
            putExtra(com.quicksmart.android.consumer_activities.PaymentActivity.EXTRA_AMOUNT,       booking.fare)
            putExtra(com.quicksmart.android.consumer_activities.PaymentActivity.EXTRA_PROVIDER_ID,  booking.providerId)
        }
        startActivity(intent)
    }

    private fun handleTrackDistance(booking: RideBookingModel) {
        TrackingBottomSheet.newInstance(booking).show(childFragmentManager, TrackingBottomSheet.TAG)
    }

    private fun handleRentCancel(booking: RentalRequestModel) {
        requireContext().showConfirmDialog(
            title = "Cancel Booking",
            message = "Are you sure you want to cancel this rent booking?"
        ) { confirmed ->
            if (confirmed) {
                db.collection("bookings").document(booking.bookingId)
                    .update("status", "cancelled")
                    .addOnSuccessListener { requireContext().showToast("Booking cancelled successfully.") }
                    .addOnFailureListener { e -> requireContext().showToast("Failed to cancel: ${e.message}") }
            }
        }
    }

    private fun handleRentPay(booking: RentalRequestModel) {
        val intent = android.content.Intent(requireContext(), com.quicksmart.android.consumer_activities.PaymentActivity::class.java).apply {
            putExtra(com.quicksmart.android.consumer_activities.PaymentActivity.EXTRA_BOOKING_ID,   booking.bookingId)
            putExtra(com.quicksmart.android.consumer_activities.PaymentActivity.EXTRA_BOOKING_TYPE, "rent")
            putExtra(com.quicksmart.android.consumer_activities.PaymentActivity.EXTRA_AMOUNT,       booking.amount)
            putExtra(com.quicksmart.android.consumer_activities.PaymentActivity.EXTRA_PROVIDER_ID,  booking.providerId)
        }
        startActivity(intent)
    }
}
