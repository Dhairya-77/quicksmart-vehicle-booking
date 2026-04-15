package com.quicksmart.android.provider_activities.adapter_model

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import android.content.Intent
import android.net.Uri
import com.quicksmart.android.R
import com.quicksmart.android.reusable_methods.toFormattedDateTime

class RentalRequestAdapter(
    private val items: List<RentalRequestModel>,
    private val onAction: (RentalRequestModel, Boolean) -> Unit
) : RecyclerView.Adapter<RentalRequestAdapter.VH>() {

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val vehicleIcon    : ImageView = itemView.findViewById(R.id.rental_req_vehicle_icon)
        val consumerName   : TextView  = itemView.findViewById(R.id.rental_req_consumer_name)
        val btnCall        : View      = itemView.findViewById(R.id.rental_req_call_container)
        val vehicleNo      : TextView  = itemView.findViewById(R.id.rental_req_vehicle_no)
        val status         : TextView  = itemView.findViewById(R.id.rental_req_status)
        val fromDate       : TextView  = itemView.findViewById(R.id.rental_req_from_date)
        val toDate         : TextView  = itemView.findViewById(R.id.rental_req_to_date)
        val pickupLocation : TextView  = itemView.findViewById(R.id.rental_req_pickup_location)
        val dropoffLocation: TextView  = itemView.findViewById(R.id.rental_req_dropoff_location)
        val passengers     : TextView  = itemView.findViewById(R.id.rental_req_passengers)
        val luggage        : TextView  = itemView.findViewById(R.id.rental_req_luggage)
        val driverStatus   : TextView  = itemView.findViewById(R.id.rental_req_driver_needed)
        val fare           : TextView  = itemView.findViewById(R.id.rental_req_fare)
        val days           : TextView  = itemView.findViewById(R.id.rental_req_days)
        val btnAccept      : Button    = itemView.findViewById(R.id.btn_accept_rental)
        val btnReject      : Button    = itemView.findViewById(R.id.btn_reject_rental)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_rental_request, parent, false))

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

        holder.btnAccept.setOnClickListener { onAction(item, true) }
        holder.btnReject.setOnClickListener { onAction(item, false) }
    }
}
