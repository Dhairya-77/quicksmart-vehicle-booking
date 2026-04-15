package com.quicksmart.android.provider_activities.adapter_model

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.maps.model.LatLng
import com.quicksmart.android.R

class NewRideRequestPopupAdapter(
    private val items   : MutableList<NewRideRequestModel>,
    private val onAccept: (NewRideRequestModel, Int) -> Unit,
    private val onReject: (NewRideRequestModel, Int) -> Unit,
    private val onLocate: (LatLng) -> Unit
) : RecyclerView.Adapter<NewRideRequestPopupAdapter.VH>() {

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val consumerName : TextView  = v.findViewById(R.id.new_req_consumer_name)
        val vehicleType  : TextView  = v.findViewById(R.id.new_req_vehicle_type)
        val persons      : TextView  = v.findViewById(R.id.new_req_persons)
        val pickup       : TextView  = v.findViewById(R.id.new_req_pickup)
        val destination  : TextView  = v.findViewById(R.id.new_req_destination)
        val locateBtn    : ImageView = v.findViewById(R.id.new_req_locate_btn)
        val callBtn      : ImageView = v.findViewById(R.id.new_req_call_btn)
        val rejectBtn    : ImageView = v.findViewById(R.id.new_req_reject_btn)
        val acceptBtn    : ImageView = v.findViewById(R.id.new_req_accept_btn)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(LayoutInflater.from(parent.context)
            .inflate(R.layout.item_new_ride_request_popup, parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]

        holder.consumerName.text = item.consumerName
        holder.vehicleType.text  = item.vehicleType
        holder.persons.text      = "${item.totalPersons}P"
        holder.pickup.text       = item.pickupAddress
        holder.destination.text  = item.destination

        holder.locateBtn.setOnClickListener {
            if (item.pickupLat != 0.0 && item.pickupLng != 0.0) {
                onLocate(LatLng(item.pickupLat, item.pickupLng))
            } else {
                Toast.makeText(it.context, "Location not available", Toast.LENGTH_SHORT).show()
            }
        }

        holder.callBtn.setOnClickListener {
            val phone = item.consumerPhone
            if (phone.isNotBlank()) {
                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                it.context.startActivity(intent)
            } else {
                Toast.makeText(it.context, "Phone number not available", Toast.LENGTH_SHORT).show()
            }
        }

        holder.acceptBtn.setOnClickListener { onAccept(item, holder.adapterPosition) }
        holder.rejectBtn.setOnClickListener { onReject(item, holder.adapterPosition) }
    }

    fun removeAt(pos: Int) {
        if (pos in items.indices) {
            items.removeAt(pos)
            notifyItemRemoved(pos)
            notifyItemRangeChanged(pos, items.size)
        }
    }
}
