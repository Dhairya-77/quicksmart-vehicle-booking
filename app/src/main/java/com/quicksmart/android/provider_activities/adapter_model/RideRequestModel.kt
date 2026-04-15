package com.quicksmart.android.provider_activities.adapter_model

data class RideRequestModel(
    val consumerName   : String,
    val pickupLocation : String,
    val destination    : String,
    val totalPersons   : Int,
    val vehicleType    : String,  // "Bike", "Car", "Rickshaw"
    val status         : String   // "Pending", "Accepted", "Rejected"
)
