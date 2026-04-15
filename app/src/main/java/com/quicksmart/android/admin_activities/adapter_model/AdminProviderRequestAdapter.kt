package com.quicksmart.android.admin_activities.adapter_model

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.quicksmart.android.R
import com.quicksmart.android.reusable_methods.openPdf
import de.hdodenhof.circleimageview.CircleImageView

/**
 * Adapter for the Admin Provider Requests screen.
 * Shows pending providers with live profile photo + Approve / Reject actions.
 */
class AdminProviderRequestAdapter(
    private val items     : MutableList<AdminProviderModel>,
    private val onApprove : (AdminProviderModel, Int) -> Unit,
    private val onReject  : (AdminProviderModel, Int) -> Unit
) : RecyclerView.Adapter<AdminProviderRequestAdapter.VH>() {

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgAvatar     : CircleImageView = itemView.findViewById(R.id.imgAvatar)
        val providerName  : TextView        = itemView.findViewById(R.id.providerName)
        val providerMobile: TextView        = itemView.findViewById(R.id.providerMobile)
        val providerEmail : TextView        = itemView.findViewById(R.id.providerEmail)
        val requestedDate : TextView        = itemView.findViewById(R.id.requestedDate)
        val statusBadge   : TextView        = itemView.findViewById(R.id.statusBadge)
        val viewAadhaar   : LinearLayout    = itemView.findViewById(R.id.viewAadhaar)
        val viewLicence   : LinearLayout    = itemView.findViewById(R.id.viewLicence)
        val approveBtn    : Button          = itemView.findViewById(R.id.approveBtn)
        val rejectBtn     : Button          = itemView.findViewById(R.id.rejectBtn)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(LayoutInflater.from(parent.context)
            .inflate(R.layout.item_admin_provider_request, parent, false))

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        val ctx  = holder.itemView.context

        // ── Profile photo (always live from Firebase Storage) ──────────────
        Glide.with(ctx)
            .load(item.profileImg.ifBlank { null })
            .placeholder(R.drawable.ic_person)
            .error(R.drawable.ic_person)
            .circleCrop()
            .into(holder.imgAvatar)

        // ── Text fields ────────────────────────────────────────────────────
        val fullName = "${item.firstName.trim()} ${item.lastName.trim()}".trim().ifBlank { "—" }
        holder.providerName.text   = fullName
        holder.providerMobile.text = item.mobile.ifBlank { "—" }
        holder.providerEmail.text  = item.email.ifBlank { "—" }
        holder.requestedDate.text  = item.createdAt.ifBlank { "—" }
        holder.statusBadge.text    = item.status.replaceFirstChar { it.uppercase() }

        // ── Document viewers ───────────────────────────────────────────────
        holder.viewAadhaar.setOnClickListener { ctx.openPdf(item.aadhaarUrl) }
        holder.viewLicence.setOnClickListener { ctx.openPdf(item.licenceUrl) }

        // ── Approve / Reject ───────────────────────────────────────────────
        holder.approveBtn.setOnClickListener { onApprove(item, holder.adapterPosition) }
        holder.rejectBtn.setOnClickListener  { onReject(item, holder.adapterPosition)  }
    }

    fun removeAt(position: Int) {
        if (position in items.indices) {
            items.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, items.size)
        }
    }
}
