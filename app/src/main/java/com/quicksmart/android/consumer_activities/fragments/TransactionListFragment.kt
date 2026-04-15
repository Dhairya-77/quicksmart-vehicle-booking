package com.quicksmart.android.consumer_activities.fragments

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.quicksmart.android.R
import com.quicksmart.android.consumer_activities.adapter_model.TransactionAdapter
import com.quicksmart.android.consumer_activities.adapter_model.TransactionModel

class TransactionListFragment : Fragment(R.layout.fragment_list_tab) {

    companion object {
        private const val ARG_TYPE = "type"
        fun newInstance(type: String) = TransactionListFragment().apply {
            arguments = Bundle().apply { putString(ARG_TYPE, type) }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val type       = arguments?.getString(ARG_TYPE) ?: "credit"
        val recycler   = view.findViewById<RecyclerView>(R.id.recycler_view)
        val emptyState = view.findViewById<LinearLayout>(R.id.layout_empty)

        recycler.layoutManager = LinearLayoutManager(requireContext())

        // ── Dummy data (replace with Firestore fetch) ─────────────────────────────
        val items = getDummyData(type)

        if (items.isEmpty()) {
            recycler.visibility   = View.GONE
            emptyState.visibility = View.VISIBLE
        } else {
            recycler.adapter      = TransactionAdapter(items)
            emptyState.visibility = View.GONE
        }
    }

    private fun getDummyData(type: String): List<TransactionModel> {
        return if (type == "credit") listOf(
            TransactionModel("Wallet Refund",     "Cancelled booking refund",    "+₹120", "28 Mar 2025", true),
            TransactionModel("Add Money",          "UPI deposit",                 "+₹500", "20 Mar 2025", true),
            TransactionModel("Cashback",           "Promo CB - QUICK10",          "+₹50",  "15 Mar 2025", true),
        ) else listOf(
            TransactionModel("Ride Payment",      "Bike ride – Rajkot",          "-₹45",  "01 Apr 2025", false),
            TransactionModel("Rental Payment",    "Car rental – 3 days",         "-₹1800","20 Mar 2025", false),
            TransactionModel("Ride Payment",      "Car ride – City Mall",        "-₹120", "28 Mar 2025", false),
        )
    }
}
