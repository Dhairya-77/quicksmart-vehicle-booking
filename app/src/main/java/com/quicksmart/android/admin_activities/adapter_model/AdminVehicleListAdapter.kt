package com.quicksmart.android.admin_activities.adapter_model

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.quicksmart.android.R
import com.quicksmart.android.reusable_methods.openPdf

/**
 * Adapter for the Admin Vehicle List screen.
 * Shows approved vehicles (read-only).
 */
class AdminVehicleListAdapter(
    private val items: List<AdminVehicleModel>
) : RecyclerView.Adapter<AdminVehicleListAdapter.VH>() {

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val vehicleIcon  : ImageView = itemView.findViewById(R.id.vehicleIcon)
        val vehicleType  : TextView  = itemView.findViewById(R.id.vehicleType)
        val vehicleNumber: TextView  = itemView.findViewById(R.id.vehicleNumber)
        val vehicleOwner : TextView  = itemView.findViewById(R.id.vehicleOwner)
        val vehiclePurpose: TextView = itemView.findViewById(R.id.vehiclePurpose)
        val approvedDate : TextView  = itemView.findViewById(R.id.approvedDate)
        val vehicleSeats : TextView  = itemView.findViewById(R.id.vehicleSeats)
        val vehiclePrice : TextView  = itemView.findViewById(R.id.vehiclePrice)
        val viewRcDocument: View     = itemView.findViewById(R.id.viewRcDocument)
        val statusBadge   : TextView = itemView.findViewById(R.id.statusBadge)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_admin_vehicle_list, parent, false))

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]

        holder.vehicleType.text   = item.vehicleType.ifBlank { "—" }
        holder.vehicleNumber.text = item.vehicleNumber.ifBlank { "—" }
        holder.vehicleOwner.text  = item.ownerName.ifBlank { "—" }
        holder.vehiclePurpose.text = item.purpose.ifBlank { "—" }
        holder.vehicleSeats.text   = if (item.seatCount == 1) "1 Seat" else "${item.seatCount} Seats"
        
        if (item.purpose == "For Rent") {
            holder.vehiclePrice.visibility = View.VISIBLE
            holder.vehiclePrice.text = "₹${item.pricePerKm} / KM"
        } else {
            holder.vehiclePrice.visibility = View.GONE
        }

        holder.approvedDate.text  = item.createdAt.ifBlank { "—" }

        // Status badge: purple for approved, grey for anything else (pending/rejected)
        holder.statusBadge.text = item.status.replaceFirstChar { it.uppercase() }
        if (item.status == "approved") {
            holder.statusBadge.backgroundTintList = holder.itemView.context.getColorStateList(R.color.green)
        } else {
            holder.statusBadge.backgroundTintList = holder.itemView.context.getColorStateList(R.color.darkGray)
        }

        holder.vehicleIcon.setImageResource(
            when {
                item.vehicleType.contains("Bike", ignoreCase = true) -> R.drawable.ic_bike
                item.vehicleType.contains("Rickshaw", ignoreCase = true) -> R.drawable.ic_rickshaw
                else -> R.drawable.ic_car
            }
        )

        holder.viewRcDocument.setOnClickListener {
            holder.itemView.context.openPdf(item.rcDocLink)
        }
    }
}
