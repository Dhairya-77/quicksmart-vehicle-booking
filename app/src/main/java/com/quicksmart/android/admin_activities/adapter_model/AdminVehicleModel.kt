package com.quicksmart.android.admin_activities.adapter_model

/**
 * Represents a vehicle record in Firestore.
 * Firestore collection: "vehicles" → document fields mapped here.
 */
data class AdminVehicleModel(
    val docId         : String = "",
    val vehicleType   : String = "",   // "Car" | "Bike / Scooter" | "Rickshaw"
    val vehicleNumber : String = "",
    val ownerName     : String = "",
    val ownerId       : String = "",   // provider's Firestore userId (for notifications + Storage path)
    val purpose       : String = "",   // "For Rent" | "For Ride"
    val seatCount     : Int    = 0,    // Total seats excluding driver
    val pricePerKm    : Double = 0.0,
    val rcDocLink     : String = "",   // Firebase Storage URL
    val status        : String = "pending",
    val createdAt     : String = ""
)
