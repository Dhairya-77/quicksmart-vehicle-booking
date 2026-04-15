package com.quicksmart.android.provider_activities.adapter_model

data class ProviderRentalBookingModel(
    val consumerName   : String,
    val vehicleType    : String,
    val vehicleNumber  : String,
    val fromDate       : String,
    val toDate         : String,
    val pickupLocation : String,
    val totalFare      : String,
    val days           : String,
    val status         : String
)
