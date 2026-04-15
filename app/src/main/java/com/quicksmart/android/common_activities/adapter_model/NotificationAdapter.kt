package com.quicksmart.android.common_activities.adapter_model

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.quicksmart.android.R

class NotificationAdapter(
    private val items: List<NotificationModel>
) : RecyclerView.Adapter<NotificationAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val title     : TextView = view.findViewById(R.id.notificationTitle)
        val message   : TextView = view.findViewById(R.id.notificationMessage)
        val time      : TextView = view.findViewById(R.id.notificationTime)
        val accentBar : View     = view.findViewById(R.id.accentBar)
        val unreadDot : View     = view.findViewById(R.id.unreadDot)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item    = items[position]
        val context = holder.itemView.context

        holder.title.text   = item.title
        holder.message.text = item.message
        holder.time.text    = item.timestamp

        if (item.isRead) {
            // Read: grey accent bar, dot hidden
            holder.accentBar.setBackgroundColor(
                ContextCompat.getColor(context, R.color.lightPurple)
            )
            holder.unreadDot.visibility = View.GONE
        } else {
            // Unread: dark purple accent bar, dot visible
            holder.accentBar.setBackgroundColor(
                ContextCompat.getColor(context, R.color.darkPurple)
            )
            holder.unreadDot.visibility = View.VISIBLE
        }
    }

    override fun getItemCount() = items.size
}
