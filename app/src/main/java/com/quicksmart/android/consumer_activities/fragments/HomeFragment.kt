package com.quicksmart.android.consumer_activities.fragments

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.quicksmart.android.R
import com.quicksmart.android.consumer_activities.ConsumerMapActivity
import com.quicksmart.android.consumer_activities.RentVehicleActivity
import com.quicksmart.android.reusable_methods.getString

class HomeFragment : Fragment(R.layout.fragment_consumer_home) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Fetching firstName from local storage (SharedPreferences)
        val firstName = requireContext().getString("firstName", "User")
        view.findViewById<TextView>(R.id.welcomeTitle)?.text = "Welcome back, $firstName"

        // Instant Ride onClick Listener
        view.findViewById<View>(R.id.cardInstantBike)?.setOnClickListener {
            startActivity(Intent(requireContext(), ConsumerMapActivity::class.java).apply {
                putExtra("vehicle_type", "bike")
            })
        }

        view.findViewById<View>(R.id.cardInstantCar)?.setOnClickListener {
            startActivity(Intent(requireContext(), ConsumerMapActivity::class.java).apply {
                putExtra("vehicle_type", "car")
            })
        }

        view.findViewById<View>(R.id.cardInstantRickshaw)?.setOnClickListener {
            startActivity(Intent(requireContext(), ConsumerMapActivity::class.java).apply {
                putExtra("vehicle_type", "rickshaw")
            })
        }

        // Rent Vehicle onClick Listener
        view.findViewById<View>(R.id.cardRentBike)?.setOnClickListener {
            startActivity(Intent(requireContext(), RentVehicleActivity::class.java).apply {
                putExtra("vehicle_type", "bike")
            })
        }

        view.findViewById<View>(R.id.cardRentCar)?.setOnClickListener {
            startActivity(Intent(requireContext(), RentVehicleActivity::class.java).apply {
                putExtra("vehicle_type", "car")
            })
        }
    }
}
