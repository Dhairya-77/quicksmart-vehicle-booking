package com.quicksmart.android.provider_activities.adapter_model

data class NewRideRequestModel(
    val bookingId     : String = "",
    val consumerId    : String = "",
    val consumerName  : String = "",
    val consumerPhone : String = "",
    val vehicleType   : String = "",
    val totalPersons  : Int    = 1,
    val pickupAddress : String = "",
    val destination   : String = "",
    val pickupLat     : Double = 0.0,
    val pickupLng     : Double = 0.0,
    val perPersonPrice: Int    = 0
)
