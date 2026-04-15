package com.quicksmart.android.provider_activities.adapter_model

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Button
import androidx.recyclerview.widget.RecyclerView
import com.quicksmart.android.R
import android.content.Intent
import android.net.Uri
import com.quicksmart.android.reusable_methods.toFormattedDateTime

import android.widget.Toast
import java.util.Calendar
import com.quicksmart.android.reusable_methods.toFormattedDate

class ProviderRentalBookingAdapter(
    private val items: List<RentalRequestModel>,
    private val onComplete: (RentalRequestModel) -> Unit
) : RecyclerView.Adapter<ProviderRentalBookingAdapter.VH>() {

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val vehicleIcon    : ImageView = itemView.findViewById(R.id.rental_book_vehicle_icon)
        val consumerName   : TextView  = itemView.findViewById(R.id.rental_book_consumer_name)
        val btnCall        : View      = itemView.findViewById(R.id.rental_book_call_container)
        val vehicleNo      : TextView  = itemView.findViewById(R.id.rental_book_vehicle_no)
        val status         : TextView  = itemView.findViewById(R.id.rental_book_status)
        val fromDate       : TextView  = itemView.findViewById(R.id.rental_book_from_date)
        val toDate         : TextView  = itemView.findViewById(R.id.rental_book_to_date)
        val pickupLocation : TextView  = itemView.findViewById(R.id.rental_book_pickup_location)
        val dropoffLocation: TextView  = itemView.findViewById(R.id.rental_book_dropoff_location)
        val fare           : TextView  = itemView.findViewById(R.id.rental_book_fare)
        val days           : TextView  = itemView.findViewById(R.id.rental_book_days)
        val passengers     : TextView  = itemView.findViewById(R.id.rental_book_passengers)
        val driverStatus   : TextView  = itemView.findViewById(R.id.rental_book_driver_needed)
        val btnComplete    : Button    = itemView.findViewById(R.id.btn_complete_trip)
        val paymentStatus  : TextView  = itemView.findViewById(R.id.rental_book_payment_status)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_provider_rental_booking, parent, false))

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]

        holder.consumerName.text   = item.consumerName
        holder.vehicleNo.text      = item.vehicleNo
        holder.status.text         = item.status.replaceFirstChar { it.uppercase() }

        holder.fromDate.text       = item.fromDate?.toFormattedDateTime() ?: "—"
        holder.toDate.text         = item.toDate?.toFormattedDateTime() ?: "—"

        holder.pickupLocation.text  = item.pickupLocation.ifBlank { "—" }
        holder.dropoffLocation.text = item.dropoffLocation.ifBlank { "—" }

        holder.fare.text = item.amount.ifBlank { "—" }
        holder.days.text = item.tripDays.ifBlank { "—" }

        // Combine passengers & luggage into a single display row
        val passText = item.passengerCount.ifBlank { "—" }
        val luggText = item.luggageCount.ifBlank { "—" }
        holder.passengers.text = "$passText / $luggText"

        if (item.withDriver) {
            holder.driverStatus.visibility = View.VISIBLE
        } else {
            holder.driverStatus.visibility = View.GONE
        }

        holder.vehicleIcon.setImageResource(
            if (item.vehicleType.contains("Bike", ignoreCase = true)) R.drawable.ic_bike
            else R.drawable.ic_car
        )

        holder.btnCall.setOnClickListener {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:${item.contactNumber}")
            }
            holder.itemView.context.startActivity(intent)
        }

        // Show payment status label
        holder.paymentStatus.visibility = View.VISIBLE
        if (item.paymentDone) {
            holder.paymentStatus.text = "Payment Received"
            holder.paymentStatus.setTextColor(
                androidx.core.content.ContextCompat.getColor(holder.itemView.context, com.quicksmart.android.R.color.green))
            holder.btnComplete.isEnabled = true
            holder.btnComplete.alpha     = 1f
        } else {
            holder.paymentStatus.text = "Waiting for Payment"
            holder.paymentStatus.setTextColor(
                androidx.core.content.ContextCompat.getColor(holder.itemView.context, com.quicksmart.android.R.color.orange))
            holder.btnComplete.isEnabled = false
            holder.btnComplete.alpha     = 0.4f
        }

        holder.btnComplete.setOnClickListener {
            val context = holder.itemView.context
            if (!item.paymentDone) {
                Toast.makeText(context, "Cannot complete: payment not received yet.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val nowCal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            
            val returnCal = Calendar.getInstance().apply {
                time = item.toDate?.toDate() ?: java.util.Date()
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            
            if (nowCal.timeInMillis >= returnCal.timeInMillis) {
                onComplete(item)
            } else {
                val dateStr = item.toDate?.toFormattedDate() ?: "the scheduled date"
                Toast.makeText(context, "This Trip will end on $dateStr", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
