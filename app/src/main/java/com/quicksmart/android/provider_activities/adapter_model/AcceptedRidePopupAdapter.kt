package com.quicksmart.android.provider_activities.adapter_model

import android.content.Intent
import android.content.res.ColorStateList
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.maps.model.LatLng
import com.quicksmart.android.R

class AcceptedRidePopupAdapter(
    private val items      : MutableList<AcceptedRideModel>,
    private val onLocate   : (LatLng) -> Unit,
    private val onPickedUp : (AcceptedRideModel, Int) -> Unit,
    private val onComplete : (AcceptedRideModel, Int) -> Unit
) : RecyclerView.Adapter<AcceptedRidePopupAdapter.VH>() {

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val consumerName   : TextView  = v.findViewById(R.id.accepted_consumer_name)
        val pickup         : TextView  = v.findViewById(R.id.accepted_pickup)
        val destination    : TextView  = v.findViewById(R.id.accepted_destination)
        val statusBadge    : TextView  = v.findViewById(R.id.accepted_status_badge)
        val personBadge    : TextView  = v.findViewById(R.id.accepted_person_count_badge)
        val locateBtn      : ImageView = v.findViewById(R.id.accepted_locate_btn)
        val callBtn        : ImageView = v.findViewById(R.id.accepted_call_btn)
        val pickupBtn      : Button    = v.findViewById(R.id.accepted_pickup_btn)
        val completeBtn    : Button    = v.findViewById(R.id.accepted_complete_btn)
        val paymentStatus  : TextView  = v.findViewById(R.id.accepted_payment_status)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(LayoutInflater.from(parent.context)
            .inflate(R.layout.item_accepted_ride_popup, parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        val ctx  = holder.itemView.context

        holder.consumerName.text = item.consumerName
        holder.pickup.text       = item.pickupAddress
        holder.destination.text  = item.destination

        // Status badge colour
        when (item.status) {
            "picked_up" -> {
                holder.statusBadge.text = "Picked Up"
                holder.statusBadge.backgroundTintList =
                    ColorStateList.valueOf(ContextCompat.getColor(ctx, R.color.green))
            }
            else -> {
                holder.statusBadge.text = "Accepted"
                holder.statusBadge.backgroundTintList =
                    ColorStateList.valueOf(ContextCompat.getColor(ctx, R.color.darkPurple))
            }
        }
        holder.personBadge.text = "${item.totalPersons}P"

        // Show Pickup button only when status == accepted
        holder.pickupBtn.visibility   = if (item.status == "accepted")  View.VISIBLE else View.GONE

        // Show Complete button only when status == picked_up
        // Gate it on paymentDone — provider can only complete after consumer pays
        if (item.status == "picked_up") {
            holder.completeBtn.visibility  = View.VISIBLE
            holder.paymentStatus.visibility = View.VISIBLE
            if (item.paymentDone) {
                holder.paymentStatus.text      = "Payment Received"
                holder.paymentStatus.setTextColor(
                    androidx.core.content.ContextCompat.getColor(ctx, R.color.green))
                holder.completeBtn.isEnabled           = true
                holder.completeBtn.alpha               = 1f
                holder.completeBtn.setOnClickListener { onComplete(item, holder.adapterPosition) }
            } else {
                holder.paymentStatus.text      = "Waiting for Payment"
                holder.paymentStatus.setTextColor(
                    androidx.core.content.ContextCompat.getColor(ctx, R.color.orange))
                holder.completeBtn.isEnabled   = false
                holder.completeBtn.alpha       = 0.4f
                holder.completeBtn.setOnClickListener(null)
            }
        } else {
            holder.completeBtn.visibility   = View.GONE
            holder.paymentStatus.visibility = View.GONE
        }

        holder.pickupBtn.setOnClickListener { onPickedUp(item, holder.adapterPosition) }

        // Locate — pan map to consumer's pickup location
        holder.locateBtn.setOnClickListener {
            if (item.pickupLat != 0.0 && item.pickupLng != 0.0) {
                onLocate(LatLng(item.pickupLat, item.pickupLng))
            } else {
                Toast.makeText(ctx, "Location not available", Toast.LENGTH_SHORT).show()
            }
        }

        // Call consumer
        holder.callBtn.setOnClickListener {
            if (item.consumerPhone.isNotBlank()) {
                ctx.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${item.consumerPhone}")))
            } else {
                Toast.makeText(ctx, "Phone number not available", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun updateAt(pos: Int, model: AcceptedRideModel) {
        if (pos in items.indices) {
            items[pos] = model
            notifyItemChanged(pos)
        }
    }

    fun removeAt(pos: Int) {
        if (pos in items.indices) {
            items.removeAt(pos)
            notifyItemRemoved(pos)
            notifyItemRangeChanged(pos, items.size)
        }
    }

    fun updateList(new: List<AcceptedRideModel>) {
        items.clear()
        items.addAll(new)
        notifyDataSetChanged()
    }
}
