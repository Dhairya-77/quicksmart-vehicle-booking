package com.quicksmart.android.admin_activities.adapter_model

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.quicksmart.android.R
import com.quicksmart.android.reusable_methods.openPdf

/**
 * Adapter for the Admin Vehicle Requests screen.
 * Shows pending vehicles with Approve / Reject actions.
 */
class AdminVehicleRequestAdapter(
    private val items     : MutableList<AdminVehicleModel>,
    private val onApprove : (AdminVehicleModel, Int) -> Unit,
    private val onReject  : (AdminVehicleModel, Int) -> Unit
) : RecyclerView.Adapter<AdminVehicleRequestAdapter.VH>() {

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val vehicleIcon   : ImageView = itemView.findViewById(R.id.vehicleIcon)
        val vehicleType   : TextView  = itemView.findViewById(R.id.vehicleType)
        val vehicleNumber : TextView  = itemView.findViewById(R.id.vehicleNumber)
        val vehicleOwner  : TextView  = itemView.findViewById(R.id.vehicleOwner)
        val vehiclePurpose: TextView  = itemView.findViewById(R.id.vehiclePurpose)
        val vehicleSeats  : TextView  = itemView.findViewById(R.id.vehicleSeats)
        val vehiclePrice  : TextView  = itemView.findViewById(R.id.vehiclePrice)
        val statusBadge   : TextView  = itemView.findViewById(R.id.statusBadge)
        val approveBtn    : Button    = itemView.findViewById(R.id.approveBtn)
        val rejectBtn     : Button    = itemView.findViewById(R.id.rejectBtn)
        val viewRcDocument: View      = itemView.findViewById(R.id.viewRcDocument)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_admin_vehicle_request, parent, false))

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        val ctx  = holder.itemView.context

        holder.vehicleType.text    = item.vehicleType.ifBlank { "—" }
        holder.vehicleNumber.text  = item.vehicleNumber.ifBlank { "—" }
        holder.vehicleOwner.text   = item.ownerName.ifBlank { "—" }
        holder.vehiclePurpose.text = item.purpose.ifBlank { "—" }
        holder.vehicleSeats.text   = if (item.seatCount == 1) "1 Seat" else "${item.seatCount} Seats"
        
        if (item.purpose == "For Rent") {
            holder.vehiclePrice.visibility = View.VISIBLE
            holder.vehiclePrice.text = "₹${item.pricePerKm} / KM"
        } else {
            holder.vehiclePrice.visibility = View.GONE
        }
        holder.statusBadge.text    = item.status.replaceFirstChar { it.uppercase() }

        // Set vehicle icon based on type
        holder.vehicleIcon.setImageResource(
            when {
                item.vehicleType.contains("Bike", ignoreCase = true) -> R.drawable.ic_bike
                item.vehicleType.contains("Rickshaw", ignoreCase = true) -> R.drawable.ic_rickshaw
                else -> R.drawable.ic_car
            }
        )

        holder.approveBtn.setOnClickListener { onApprove(item, holder.adapterPosition) }
        holder.rejectBtn.setOnClickListener  { onReject(item, holder.adapterPosition)  }
        holder.viewRcDocument.setOnClickListener { ctx.openPdf(item.rcDocLink) }
    }

    fun removeAt(position: Int) {
        if (position in items.indices) {
            items.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, items.size)
        }
    }
}
