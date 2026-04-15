package com.quicksmart.android.admin_activities.adapter_model

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.quicksmart.android.R
import com.quicksmart.android.reusable_methods.openPdf
import de.hdodenhof.circleimageview.CircleImageView

/**
 * Adapter for the Admin Provider List screen.
 * Shows approved providers (read-only) with live profile photo.
 */
class AdminProviderListAdapter(
    private val items: List<AdminProviderModel>
) : RecyclerView.Adapter<AdminProviderListAdapter.VH>() {

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgAvatar     : CircleImageView = itemView.findViewById(R.id.imgAvatar)
        val providerName  : TextView        = itemView.findViewById(R.id.providerName)
        val providerMobile: TextView        = itemView.findViewById(R.id.providerMobile)
        val providerEmail : TextView        = itemView.findViewById(R.id.providerEmail)
        val joinDate      : TextView        = itemView.findViewById(R.id.joinDate)
        val statusBadge   : TextView        = itemView.findViewById(R.id.statusBadge)
        val viewAadhaar   : LinearLayout    = itemView.findViewById(R.id.viewAadhaar)
        val licenceView   : LinearLayout    = itemView.findViewById(R.id.viewLicence)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(LayoutInflater.from(parent.context)
            .inflate(R.layout.item_admin_provider_list, parent, false))

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
        holder.joinDate.text       = item.createdAt.ifBlank { "—" }
        holder.statusBadge.text    = item.status.replaceFirstChar { it.uppercase() }

        // ── Document viewers ───────────────────────────────────────────────
        holder.viewAadhaar.setOnClickListener { ctx.openPdf(item.aadhaarUrl) }
        holder.licenceView.setOnClickListener { ctx.openPdf(item.licenceUrl) }
    }
}
