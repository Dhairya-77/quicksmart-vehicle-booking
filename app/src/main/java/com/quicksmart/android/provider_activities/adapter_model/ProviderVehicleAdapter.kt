package com.quicksmart.android.provider_activities.adapter_model

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.quicksmart.android.R

class ProviderVehicleAdapter(
    private val items          : MutableList<ProviderVehicleModel>,
    private val onDelete       : (ProviderVehicleModel, Int) -> Unit,
    private val onUpdatePurpose: (ProviderVehicleModel, String) -> Unit,
    private val onUpdatePrice  : (ProviderVehicleModel, Double) -> Unit
) : RecyclerView.Adapter<ProviderVehicleAdapter.VH>() {

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val vehicleIcon        : ImageView = itemView.findViewById(R.id.vehicle_icon)
        val vehicleTypeLabel   : TextView  = itemView.findViewById(R.id.vehicle_type_label)
        val vehicleNumberLabel : TextView  = itemView.findViewById(R.id.vehicle_number_label)
        val statusBadge        : TextView  = itemView.findViewById(R.id.vehicle_status_badge)
        val purposeBadge       : TextView  = itemView.findViewById(R.id.vehicle_purpose_badge)
        val purposeSpinner     : Spinner   = itemView.findViewById(R.id.vehicle_edit_purpose_spinner)
        val seatsLabel         : TextView  = itemView.findViewById(R.id.vehicle_seats_label)
        val layoutPricing      : View      = itemView.findViewById(R.id.layout_pricing_edit)
        val priceInput         : android.widget.EditText = itemView.findViewById(R.id.vehicle_edit_price_input)
        val btnDelete          : Button    = itemView.findViewById(R.id.btn_delete_vehicle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_provider_vehicle, parent, false))

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]

        holder.vehicleTypeLabel.text   = item.vehicleType
        holder.vehicleNumberLabel.text = item.vehicleNumber
        holder.purposeBadge.text       = item.purpose
        holder.seatsLabel.text         = if (item.seatCount == 1) "1 Seat" else "${item.seatCount} Seats"
        
        // Status badge: light grey for pending, dark purple for approved
        holder.statusBadge.text = item.status.replaceFirstChar { it.uppercase() }
        if (item.status == "approved") {
            holder.statusBadge.setBackgroundResource(R.drawable.bg_bottom_sheet_rounded)
            holder.statusBadge.backgroundTintList = holder.itemView.context.getColorStateList(R.color.green)
        } else {
            holder.statusBadge.setBackgroundResource(R.drawable.bg_bottom_sheet_rounded)
            holder.statusBadge.backgroundTintList = holder.itemView.context.getColorStateList(R.color.darkPurple)
        }

        holder.vehicleIcon.setImageResource(
            when {
                item.vehicleType.contains("Bike", ignoreCase = true) -> R.drawable.ic_bike
                item.vehicleType.contains("Rickshaw", ignoreCase = true) -> R.drawable.ic_rickshaw
                else -> R.drawable.ic_car
            }
        )
        
        // Pricing visibility
        if (item.purpose == "For Rent") {
            holder.layoutPricing.visibility = View.VISIBLE
            holder.priceInput.setText(item.pricePerKm.toString())
            
            // Handle price edit done
            holder.priceInput.setOnEditorActionListener { v, actionId, _ ->
                if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                    val newPrice = v.text.toString().toDoubleOrNull() ?: 0.0
                    if (newPrice != item.pricePerKm) {
                        onUpdatePrice(item, newPrice)
                    }
                    v.clearFocus()
                    true
                } else false
            }
        } else {
            holder.layoutPricing.visibility = View.GONE
        }

        // Spinner logic for editing purpose
        val purposes = listOf("For Rent", "For Ride")
        val spinnerAdapter = ArrayAdapter(holder.itemView.context, R.layout.spinner_selected_item, purposes)
            .also { it.setDropDownViewResource(R.layout.spinner_dropdown_item) }
        holder.purposeSpinner.adapter = spinnerAdapter
        
        // Block feedback loop: set selection without triggering the listener
        holder.purposeSpinner.onItemSelectedListener = null
        val currentIdx = purposes.indexOf(item.purpose)
        if (currentIdx != -1) holder.purposeSpinner.setSelection(currentIdx)

        holder.purposeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                val newPurpose = purposes[pos]
                if (newPurpose != item.purpose) {
                    onUpdatePurpose(item, newPurpose)
                    holder.purposeBadge.text = newPurpose
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        holder.btnDelete.setOnClickListener {
            onDelete(item, holder.adapterPosition)
        }
    }

    fun removeAt(position: Int) {
        if (position in items.indices) {
            items.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, items.size)
        }
    }
}
