package com.quicksmart.android.consumer_activities.adapter_model

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.quicksmart.android.R

/**
 * Adapter for the rental-vehicle picker bottom sheet.
 * Shows vehicle name, registration, and per-km rate.
 * No ETA, no passenger count.
 */
class RentalVehicleAdapter(
    private var vehicles: List<RentalVehicleModel>,
    private val onVehicleSelected: (RentalVehicleModel) -> Unit
) : RecyclerView.Adapter<RentalVehicleAdapter.VehicleViewHolder>() {

    inner class VehicleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val vehicleIcon : ImageView = itemView.findViewById(R.id.rentalVehicleIcon)
        val nameText    : TextView  = itemView.findViewById(R.id.rentalVehicleName)
        val numberText  : TextView  = itemView.findViewById(R.id.rentalVehicleNumber)
        val rateText    : TextView  = itemView.findViewById(R.id.rentalVehicleRate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VehicleViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_rental_vehicle, parent, false)
        return VehicleViewHolder(view)
    }

    override fun onBindViewHolder(holder: VehicleViewHolder, position: Int) {
        val vehicle = vehicles[position]

        holder.nameText.text   = vehicle.ownerName
        holder.numberText.text = vehicle.vehicleNo
        holder.rateText.text   = "${vehicle.perKmRate}/km"

        holder.vehicleIcon.setImageResource(
            if (vehicle.vehicleType == "bike") R.drawable.ic_bike else R.drawable.ic_car
        )

        holder.itemView.setOnClickListener { onVehicleSelected(vehicle) }
    }

    override fun getItemCount(): Int = vehicles.size

    fun updateList(newVehicles: List<RentalVehicleModel>) {
        vehicles = newVehicles
        notifyDataSetChanged()
    }
}
