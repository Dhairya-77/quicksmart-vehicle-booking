package com.quicksmart.android.consumer_activities.adapter_model

/**
 * Represents a single rental vehicle shown in the rental bottom sheet.
 *
 * @param vehicleId    Firestore document ID (empty for dummy data)
 * @param vehicleName  Display name e.g. "Hero Splendor+"
 * @param vehicleType  "bike" | "car"
 * @param vehicleNo    Registration number e.g. "GJ 01 AB 1234"
 * @param perKmRate    Fare per kilometre e.g. "₹12"
 */
data class RentalVehicleModel(
    val vehicleId  : String = "",
    val ownerName  : String = "",   // Changed from vehicleName
    val ownerId    : String = "",
    val vehicleType: String = "",   // "bike" | "car"
    val vehicleNo  : String = "",
    val perKmRate  : String = ""    // e.g. "₹12"
)
