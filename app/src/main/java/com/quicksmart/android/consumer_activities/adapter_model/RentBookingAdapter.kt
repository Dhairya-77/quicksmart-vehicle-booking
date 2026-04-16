package com.quicksmart.android.consumer_activities.adapter_model

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Timestamp
import com.quicksmart.android.R
import com.quicksmart.android.provider_activities.adapter_model.RentalRequestModel
import com.quicksmart.android.reusable_methods.toFormattedDateTime
import java.text.SimpleDateFormat
import java.util.Locale

import android.widget.Button

/**
 * RecyclerView adapter for the Rent tab inside the consumer Bookings fragment.
 */
class RentBookingAdapter(
    private var bookings: List<RentalRequestModel>,
    private val onCancel: (RentalRequestModel) -> Unit,
    private val onPay: (RentalRequestModel) -> Unit
) : RecyclerView.Adapter<RentBookingAdapter.RentViewHolder>() {

    inner class RentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val vehicleIcon    : ImageView = itemView.findViewById(R.id.rentVehicleIcon)
        val ownerName      : TextView  = itemView.findViewById(R.id.rentOwnerName)
        val vehicleNo      : TextView  = itemView.findViewById(R.id.rentVehicleNo)
        val statusBadge    : TextView  = itemView.findViewById(R.id.rentStatusBadge)

        val fromDate       : TextView  = itemView.findViewById(R.id.rentFromDate)
        val toDate         : TextView  = itemView.findViewById(R.id.rentToDate)

        val pickupLocation : TextView  = itemView.findViewById(R.id.rentPickupLocation)
        val dropoffLocation: TextView  = itemView.findViewById(R.id.rentDropoffLocation)
        
        val totalFare      : TextView  = itemView.findViewById(R.id.rentTotalFare)
        val days           : TextView  = itemView.findViewById(R.id.rentDays)
        val passengers     : TextView  = itemView.findViewById(R.id.rentPassengers)
        val driverInfo     : TextView  = itemView.findViewById(R.id.rentDriverInfo)

        val actionDivider   : View      = itemView.findViewById(R.id.rentActionDivider)
        val actionLayout    : View      = itemView.findViewById(R.id.rentActionLayout)
        val btnCancel       : Button    = itemView.findViewById(R.id.btnRentCancel)
        val btnPay          : Button    = itemView.findViewById(R.id.btnRentPay)
        val paymentDoneLabel: TextView  = itemView.findViewById(R.id.rentPaymentDoneLabel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_rent_booking, parent, false)
        return RentViewHolder(view)
    }

    override fun onBindViewHolder(holder: RentViewHolder, position: Int) {
        val booking = bookings[position]

        // Basic fields
        holder.ownerName.text      = booking.providerName.ifEmpty { "Unknown Owner" }
        holder.vehicleNo.text      = booking.vehicleNo
        
        // Hide time fields and use Date field to show both using toFormattedDateTime,
        // but wait! The new layout puts Date and Time in the same TextView, maybe?
        // Actually, let's just format it into a single string.
        holder.fromDate.text       = booking.fromDate?.toFormattedDateTime() ?: "—"
        holder.toDate.text         = booking.toDate?.toFormattedDateTime() ?: "—"
        
        holder.pickupLocation.text = booking.pickupLocation
        holder.dropoffLocation.text = booking.dropoffLocation
        holder.totalFare.text      = booking.amount
        holder.days.text           = booking.tripDays
        holder.passengers.text     = "${booking.passengerCount} P / ${booking.luggageCount} B"

        if (booking.withDriver) {
            holder.driverInfo.visibility = View.VISIBLE
        } else {
            holder.driverInfo.visibility = View.GONE
        }

        // Vehicle icon
        holder.vehicleIcon.setImageResource(
            if (booking.vehicleType.contains("bike", true)) R.drawable.ic_bike
            else R.drawable.ic_car
        )

        // Status badge colour (User requested pendig -> red, accepted -> green)
        val ctx = holder.itemView.context
        val (badgeLabel, badgeColor) = when (booking.status) {
            "accepted"  -> "Accepted"  to ContextCompat.getColor(ctx, R.color.green)
            "pending"   -> "Pending"   to ContextCompat.getColor(ctx, R.color.red)
            "completed" -> "Completed" to ContextCompat.getColor(ctx, R.color.darkGray)
            "cancelled" -> "Cancelled" to ContextCompat.getColor(ctx, R.color.red)
            "ongoing"   -> "Ongoing"   to ContextCompat.getColor(ctx, R.color.darkPurple)
            else        -> booking.status.replaceFirstChar { it.uppercase() } to ContextCompat.getColor(ctx, R.color.darkGray)
        }
        holder.statusBadge.text = badgeLabel
        holder.statusBadge.backgroundTintList =
            android.content.res.ColorStateList.valueOf(badgeColor)

        // Reset buttons visibility
        holder.btnCancel.visibility = View.INVISIBLE
        holder.btnPay.visibility = View.INVISIBLE
        holder.actionDivider.visibility = View.GONE
        holder.actionLayout.visibility = View.GONE

        // Helper to get normalized date
        fun getNormalizedTime(timestamp: Timestamp?): Long {
            if (timestamp == null) return 0L
            return java.util.Calendar.getInstance().apply {
                time = timestamp.toDate()
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }.timeInMillis
        }

        val nowNormalized = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis

        var hasActionButtons = false

        // Cancel button visibility: before pick up date
        if (booking.status == "pending" || booking.status == "accepted") {
            if (booking.fromDate != null) {
                val fromNormalized = getNormalizedTime(booking.fromDate)
                if (nowNormalized < fromNormalized) {
                    holder.btnCancel.visibility = View.VISIBLE
                    hasActionButtons = true
                }
            }
        }

        // Pay button: on/after return date AND not yet paid → show Pay
        //             already paid → show green label, hide button
        if (booking.status == "pending" || booking.status == "accepted") {
            if (booking.toDate != null) {
                val toNormalized = getNormalizedTime(booking.toDate)
                if (nowNormalized >= toNormalized) {
                    if (booking.paymentDone) {
                        holder.btnPay.visibility          = View.INVISIBLE
                        holder.paymentDoneLabel.visibility = View.VISIBLE
                        hasActionButtons = true
                    } else {
                        holder.btnPay.visibility          = View.VISIBLE
                        holder.paymentDoneLabel.visibility = View.GONE
                        hasActionButtons = true
                    }
                }
            }
        }

        if (hasActionButtons) {
            holder.actionDivider.visibility = View.VISIBLE
            holder.actionLayout.visibility = View.VISIBLE
        }

        holder.btnCancel.setOnClickListener { onCancel(booking) }
        holder.btnPay.setOnClickListener { onPay(booking) }
    }

    override fun getItemCount(): Int = bookings.size

    fun updateList(newBookings: List<RentalRequestModel>) {
        bookings = newBookings
        notifyDataSetChanged()
    }
}
