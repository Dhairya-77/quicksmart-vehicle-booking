package com.quicksmart.android.consumer_activities.adapter_model

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.quicksmart.android.R

import java.text.SimpleDateFormat
import java.util.Locale

/**
 * RecyclerView adapter for the Ride tab inside the consumer Bookings fragment.
 *
 * Statuses used:
 *   pending    → show Cancel
 *   accepted   → show Cancel
 *   picked_up  → show Pay Now
 *   completed  → no actions
 *   cancelled / rejected → no actions
 */
class RideBookingAdapter(
    private var bookings        : MutableList<RideBookingModel>,
    private val onCancel        : (RideBookingModel, Int) -> Unit,
    private val onPayNow        : (RideBookingModel) -> Unit,
    private val onTrackDistance : (RideBookingModel) -> Unit = {}
) : RecyclerView.Adapter<RideBookingAdapter.RideViewHolder>() {

    private val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

    // ── ViewHolder ────────────────────────────────────────────────────────
    inner class RideViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val vehicleIcon      : ImageView = itemView.findViewById(R.id.rideVehicleIcon)
        val riderName        : TextView  = itemView.findViewById(R.id.rideRiderName)
        val vehicleNo        : TextView  = itemView.findViewById(R.id.rideVehicleNo)
        val statusBadge      : TextView  = itemView.findViewById(R.id.rideStatusBadge)
        val personBadge      : TextView  = itemView.findViewById(R.id.ridePersonCountBadge)
        val fromAddress      : TextView  = itemView.findViewById(R.id.rideFromAddress)
        val toAddress        : TextView  = itemView.findViewById(R.id.rideToAddress)
        val fare             : TextView  = itemView.findViewById(R.id.rideFare)
        val dateTime         : TextView  = itemView.findViewById(R.id.rideDateTime)
        val rideETA          : TextView  = itemView.findViewById(R.id.rideETA)
        val btnCancelRide    : Button    = itemView.findViewById(R.id.btnCancelRide)
        val btnTrackDistance : Button    = itemView.findViewById(R.id.btnTrackDistance)
        val btnPayNow        : Button    = itemView.findViewById(R.id.btnPayNow)
        val paymentDoneLabel : TextView  = itemView.findViewById(R.id.ridePaymentDoneLabel)
    }

    // ── Adapter overrides ─────────────────────────────────────────────────
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RideViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ride_booking, parent, false)
        return RideViewHolder(view)
    }

    override fun onBindViewHolder(holder: RideViewHolder, position: Int) {
        val booking = bookings[position]
        val ctx     = holder.itemView.context

        // Basic info
        holder.riderName.text   = booking.riderName
        holder.vehicleNo.text   = booking.vehicleNo
        holder.fromAddress.text = booking.fromAddress
        holder.toAddress.text   = booking.toAddress
        holder.fare.text        = booking.fare
        holder.dateTime.text    = booking.bookingDate?.let { dateFormat.format(it.toDate()) } ?: "—"
        holder.personBadge.text = "${booking.totalPersons}P"

        // Vehicle icon
        holder.vehicleIcon.setImageResource(
            when (booking.vehicleType.lowercase()) {
                "bike"     -> R.drawable.ic_bike
                "rickshaw" -> R.drawable.ic_rickshaw
                else       -> R.drawable.ic_car
            }
        )

        // ── Status badge ──────────────────────────────────────────────────
        val (label, color) = when (booking.status) {
            "pending"   -> "Pending"   to ContextCompat.getColor(ctx, R.color.orange)
            "accepted"  -> "Accepted"  to ContextCompat.getColor(ctx, R.color.darkPurple)
            "picked_up" -> "On Way"    to ContextCompat.getColor(ctx, R.color.blue)
            "completed" -> "Completed" to ContextCompat.getColor(ctx, R.color.green)
            "cancelled" -> "Cancelled" to ContextCompat.getColor(ctx, R.color.darkGray)
            "rejected"  -> "Rejected"  to ContextCompat.getColor(ctx, R.color.red)
            else        -> booking.status to ContextCompat.getColor(ctx, R.color.darkGray)
        }
        holder.statusBadge.text                 = label
        holder.statusBadge.backgroundTintList   = ColorStateList.valueOf(color)

        // ── Cancel button: visible while pending or accepted (before pickup) ──
        val canCancel = booking.status == "pending" || booking.status == "accepted"
        holder.btnCancelRide.visibility = if (canCancel) View.VISIBLE else View.GONE
        if (canCancel) {
            holder.btnCancelRide.setOnClickListener {
                onCancel(booking, holder.adapterPosition)
            }
        }

        // ── Track distance: available after accepted ──────────────────────
        val canTrack = booking.status == "accepted" || booking.status == "picked_up"
        holder.btnTrackDistance.visibility = if (canTrack) View.VISIBLE else View.GONE
        if (canTrack) {
            holder.btnTrackDistance.setOnClickListener { onTrackDistance(booking) }
        }

        // ── Pay Now / Payment Done ────────────────────────────────────────
        // Once the consumer has paid, hide the button and show a green label instead.
        // The provider's "Complete Ride" button is gated on paymentDone server-side.
        val canPay = booking.status == "picked_up"
        if (canPay) {
            if (booking.paymentDone) {
                holder.btnPayNow.visibility        = View.GONE
                holder.paymentDoneLabel.visibility = View.VISIBLE
            } else {
                holder.btnPayNow.visibility        = View.VISIBLE
                holder.paymentDoneLabel.visibility = View.GONE
                holder.btnPayNow.setOnClickListener { onPayNow(booking) }
            }
        } else {
            holder.btnPayNow.visibility        = View.GONE
            holder.paymentDoneLabel.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int = bookings.size

    // ── Public API ────────────────────────────────────────────────────────
    fun updateList(newBookings: List<RideBookingModel>) {
        bookings = newBookings.toMutableList()
        notifyDataSetChanged()
    }

    fun removeAt(pos: Int) {
        if (pos in bookings.indices) {
            bookings.removeAt(pos)
            notifyItemRemoved(pos)
            notifyItemRangeChanged(pos, bookings.size)
        }
    }
}
