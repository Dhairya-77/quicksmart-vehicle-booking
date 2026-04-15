package com.quicksmart.android.provider_activities.adapter_model

data class AcceptedRideModel(
    val bookingId      : String = "",
    val consumerId     : String = "",
    val consumerName   : String = "",
    val pickupAddress  : String = "",
    val destination    : String = "",
    val consumerPhone  : String = "",
    val vehicleType    : String = "",
    val totalPersons   : Int    = 1,
    val perPersonPrice : Int    = 0,
    val pickupLat      : Double = 0.0,
    val pickupLng      : Double = 0.0,
    val status         : String = "accepted",  // accepted | picked_up
    val paymentDone    : Boolean = false        // true after consumer pays
)
