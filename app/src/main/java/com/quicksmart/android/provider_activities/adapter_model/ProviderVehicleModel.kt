package com.quicksmart.android.provider_activities.adapter_model

data class ProviderVehicleModel(
    val docId        : String = "",
    val vehicleType  : String = "",
    val vehicleNumber: String = "",
    val purpose      : String = "",   // "For Rent" | "For Ride"
    val seatCount    : Int    = 0,
    val pricePerKm   : Double = 0.0,
    val status       : String = "pending" // "pending" | "approved"
)
