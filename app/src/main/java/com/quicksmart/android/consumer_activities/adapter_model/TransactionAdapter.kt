package com.quicksmart.android.consumer_activities.adapter_model

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.quicksmart.android.R

class TransactionAdapter(private val items: List<TransactionModel>) :
    RecyclerView.Adapter<TransactionAdapter.VH>() {

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val icon:    ImageView = v.findViewById(R.id.txn_icon)
        val title:   TextView  = v.findViewById(R.id.txn_title)
        val date:    TextView  = v.findViewById(R.id.txn_date)
        val amount:  TextView  = v.findViewById(R.id.txn_amount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
        LayoutInflater.from(parent.context).inflate(R.layout.item_transaction, parent, false)
    )

    override fun getItemCount() = items.size

    override fun onBindViewHolder(h: VH, pos: Int) {
        val item = items[pos]
        h.title.text  = item.title
        h.date.text   = "${item.subtitle} · ${item.date}"
        h.amount.text = item.amount

        // Green for credit, red for debit
        val amountColor = if (item.isCredit) Color.parseColor("#1B5E20") else Color.parseColor("#B71C1C")
        h.amount.setTextColor(amountColor)

        // Icon tint matches amount colour
        h.icon.setColorFilter(amountColor)

        // Icon resource
        h.icon.setImageResource(
            if (item.isCredit) R.drawable.ic_wallet else R.drawable.ic_transaction
        )
    }
}
