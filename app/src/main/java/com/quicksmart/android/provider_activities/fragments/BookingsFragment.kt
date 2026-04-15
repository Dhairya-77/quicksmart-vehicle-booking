package com.quicksmart.android.provider_activities.fragments

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.quicksmart.android.R

class BookingsFragment : Fragment(R.layout.fragment_content) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<TextView>(R.id.greeting_text).text = "Provider Bookings"
    }
}
