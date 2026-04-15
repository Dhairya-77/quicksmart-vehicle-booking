package com.quicksmart.android.consumer_activities.adapter_model

/**
 * Represents a single available rider shown in the vehicle-picker bottom sheet.
 *
 * @param riderId        Firestore document ID (empty string for dummy data)
 * @param riderName      Display name of the rider
 * @param vehicleType    "bike" | "rickshaw" | "car" — used to filter the list
 * @param vehicleNo      Registration number e.g. "GJ01 AB 1234"
 * @param perPersonRate  Fixed fare per person e.g. "₹ 25"
 * @param maxPassengers  Capacity of the vehicle (bike=1, rickshaw=3, car=4)
 * @param eta            Estimated arrival in minutes
 */
data class RiderModel(
    val riderId      : String = "",
    val riderName    : String = "",
    val vehicleType  : String = "",   // "bike" | "rickshaw" | "car"
    val vehicleNo    : String = "",
    val perPersonRate: String = "",   // e.g. "₹ 25"
    val maxPassengers: Int    = 1,
    var eta          : Int    = 0     // minutes away
)
