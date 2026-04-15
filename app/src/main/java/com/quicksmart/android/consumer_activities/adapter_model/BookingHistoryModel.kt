package com.quicksmart.android.consumer_activities.adapter_model

data class BookingHistoryModel(
    val rideType:  String,
    val from:      String,
    val to:        String,
    val amount:    String,
    val duration:  String,
    val status:    String,
    val date:      String,
    val isRental:  Boolean,
    val consumerName: String,
    val providerName: String,
    val vehicleNo: String
)
