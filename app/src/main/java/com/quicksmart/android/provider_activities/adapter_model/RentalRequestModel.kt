package com.quicksmart.android.provider_activities.adapter_model

import com.google.firebase.Timestamp

data class RentalRequestModel(
    val bookingId      : String = "",
    val consumerId     : String = "",
    val consumerName   : String = "",
    val vehicleType    : String = "",
    val providerName   : String = "",
    val vehicleNo      : String = "",
    val fromDate       : Timestamp? = null,
    val toDate         : Timestamp? = null,
    val pickupLocation : String = "",
    val dropoffLocation: String = "",
    val amount         : String = "",
    val tripDays       : String = "",
    val status         : String = "pending",
    val contactNumber  : String = "",
    val providerId     : String = "",
    val passengerCount : String = "",
    val luggageCount   : String = "",
    val withDriver     : Boolean = false,
    val paymentDone    : Boolean = false,   // true once consumer has paid via Cashfree
    val bookingDate    : Timestamp? = null
)
