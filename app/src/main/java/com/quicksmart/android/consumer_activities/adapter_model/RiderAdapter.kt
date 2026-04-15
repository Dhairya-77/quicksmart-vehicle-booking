package com.quicksmart.android.consumer_activities.adapter_model

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.quicksmart.android.R

/**
 * RecyclerView adapter for displaying available riders in the vehicle-picker bottom sheet.
 *
 * Usage:
 *   val adapter = RiderAdapter(filteredList, selectedPassengers) { rider -> /* handle selection */ }
 *   recyclerView.adapter = adapter
 *
 * To update data from Firebase later:
 *   adapter.updateList(newList)
 */
class RiderAdapter(
    private var riders: List<RiderModel>,
    private var selectedPassengers: Int = 1,
    private val onRiderSelected: (RiderModel) -> Unit
) : RecyclerView.Adapter<RiderAdapter.RiderViewHolder>() {

    // ── ViewHolder ────────────────────────────────────────────────────────
    inner class RiderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val vehicleIcon    : ImageView = itemView.findViewById(R.id.riderVehicleIcon)
        val riderNameText  : TextView  = itemView.findViewById(R.id.riderName)
        val vehicleNumber  : TextView  = itemView.findViewById(R.id.riderVehicleNumber)
        val priceText      : TextView  = itemView.findViewById(R.id.riderPrice)
        val etaText        : TextView  = itemView.findViewById(R.id.riderEta)
        val passengerCount : TextView  = itemView.findViewById(R.id.riderPassengerCount)
    }

    // ── Adapter overrides ─────────────────────────────────────────────────
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RiderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_rider, parent, false)
        return RiderViewHolder(view)
    }

    override fun onBindViewHolder(holder: RiderViewHolder, position: Int) {
        val rider = riders[position]

        holder.riderNameText.text = rider.riderName
        holder.vehicleNumber.text = rider.vehicleNo
        holder.priceText.text     = "${rider.perPersonRate}/person"
        holder.etaText.text       = "${rider.eta} min away"

        // Show correct icon based on vehicle type
        holder.vehicleIcon.setImageResource(
            when (rider.vehicleType) {
                "bike"     -> R.drawable.ic_bike
                "rickshaw" -> R.drawable.ic_rickshaw
                else       -> R.drawable.ic_car
            }
        )

        // Show passenger occupancy for rickshaw / car (e.g. "2/3 seats")
        if (rider.vehicleType == "bike") {
            holder.passengerCount.visibility = View.GONE
        } else {
            holder.passengerCount.visibility = View.VISIBLE
            holder.passengerCount.text       = "$selectedPassengers/${rider.maxPassengers} seats"
        }

        // Card click → pass rider up
        holder.itemView.setOnClickListener {
            onRiderSelected(rider)
        }
    }

    override fun getItemCount(): Int = riders.size

    // ── Public API ────────────────────────────────────────────────────────
    /** Update passenger count (called when spinner changes) and refresh list. */
    fun setPassengers(count: Int) {
        selectedPassengers = count
        notifyDataSetChanged()
    }

    /** Call this when Firebase returns live data to refresh the list. */
    fun updateList(newRiders: List<RiderModel>) {
        riders = newRiders
        notifyDataSetChanged()
    }
}
