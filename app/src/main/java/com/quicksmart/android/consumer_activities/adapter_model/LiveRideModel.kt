package com.quicksmart.android.consumer_activities.adapter_model

/**
 * Represents a provider's active instant ride entry from Firebase Realtime Database.
 * Path: live_rides/{providerId}
 */
data class LiveRideModel(
    val providerId      : String = "",
    val providerName    : String = "",
    val vehicleNo       : String = "",
    val vehicleType     : String = "",
    val perPersonPrice  : Int    = 0,
    val totalSeats      : Int    = 0,
    val seatsOccupied   : Int    = 0,
    val seatsAvailable  : Int    = 0,
    val currentLocation : String = "",
    val currentLat      : Double = 0.0,
    val currentLng      : Double = 0.0,
    val destination     : String = "",
    val isActive        : Boolean = false,
    val updatedAt       : Long   = 0L
)
