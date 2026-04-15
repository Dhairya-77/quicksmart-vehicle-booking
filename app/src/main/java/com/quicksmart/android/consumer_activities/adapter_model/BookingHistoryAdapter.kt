package com.quicksmart.android.consumer_activities.adapter_model

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.quicksmart.android.R

class BookingHistoryAdapter(private val items: List<BookingHistoryModel>) :
    RecyclerView.Adapter<BookingHistoryAdapter.VH>() {

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val icon:     ImageView = v.findViewById(R.id.hist_vehicle_icon)
        val rideType: TextView  = v.findViewById(R.id.hist_ride_type)
        val date:     TextView  = v.findViewById(R.id.hist_date)
        val status:   TextView  = v.findViewById(R.id.hist_status)
        val from:     TextView  = v.findViewById(R.id.hist_from)
        val to:       TextView  = v.findViewById(R.id.hist_to)
        val amount:   TextView  = v.findViewById(R.id.hist_amount)
        val duration: TextView  = v.findViewById(R.id.hist_duration)
        val consumerName: TextView = v.findViewById(R.id.hist_consumer_name)
        val providerName: TextView = v.findViewById(R.id.hist_provider_name)
        val vehicleNo: TextView = v.findViewById(R.id.hist_vehicle_no)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
        LayoutInflater.from(parent.context).inflate(R.layout.item_booking_history, parent, false)
    )

    override fun getItemCount() = items.size

    override fun onBindViewHolder(h: VH, pos: Int) {
        val item = items[pos]
        h.rideType.text = item.rideType
        h.date.text     = item.date
        h.status.text   = item.status
        h.from.text     = item.from
        h.to.text       = item.to
        h.amount.text   = item.amount
        h.duration.text = item.duration
        h.consumerName.text = item.consumerName
        h.providerName.text = item.providerName
        h.vehicleNo.text = item.vehicleNo

        // Icon
        h.icon.setImageResource(
            if (item.isRental) R.drawable.ic_car else R.drawable.ic_bike
        )

        // Status badge tint
        val tintColor = when (item.status.lowercase()) {
            "completed"  -> Color.parseColor("#1B5E20")
            "cancelled"  -> Color.parseColor("#B71C1C")
            "rejected"   -> Color.parseColor("#D32F2F")
            else         -> Color.parseColor("#1565C0")
        }
        h.status.backgroundTintList = android.content.res.ColorStateList.valueOf(tintColor)
    }
}
