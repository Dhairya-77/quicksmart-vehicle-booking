package com.quicksmart.android.provider_activities.adapter_model

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.quicksmart.android.R

class RideRequestAdapter(
    private val items: List<RideRequestModel>
) : RecyclerView.Adapter<RideRequestAdapter.VH>() {

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val vehicleIcon      : ImageView = itemView.findViewById(R.id.request_vehicle_icon)
        val consumerName     : TextView  = itemView.findViewById(R.id.consumer_name)
        val totalPersons     : TextView  = itemView.findViewById(R.id.total_persons)
        val requestStatus    : TextView  = itemView.findViewById(R.id.request_status)
        val pickupLocation   : TextView  = itemView.findViewById(R.id.pickup_location)
        val destinationLabel : TextView  = itemView.findViewById(R.id.destination_location)
        val btnAccept        : Button    = itemView.findViewById(R.id.btn_accept)
        val btnReject        : Button    = itemView.findViewById(R.id.btn_reject)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_ride_request, parent, false))

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]

        holder.consumerName.text  = item.consumerName
        holder.totalPersons.text  = "${item.totalPersons} person(s) · ${item.vehicleType}"
        holder.requestStatus.text = item.status
        holder.pickupLocation.text    = item.pickupLocation
        holder.destinationLabel.text  = item.destination

        // Vehicle icon
        holder.vehicleIcon.setImageResource(
            when (item.vehicleType.lowercase()) {
                "bike", "bike / scooter" -> R.drawable.ic_bike
                "rickshaw"               -> R.drawable.ic_rickshaw
                else                     -> R.drawable.ic_car
            }
        )

        holder.btnAccept.setOnClickListener {
            Toast.makeText(it.context, "Accepted: ${item.consumerName}", Toast.LENGTH_SHORT).show()
        }
        holder.btnReject.setOnClickListener {
            Toast.makeText(it.context, "Rejected: ${item.consumerName}", Toast.LENGTH_SHORT).show()
        }
    }
}
