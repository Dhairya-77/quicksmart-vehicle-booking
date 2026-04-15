package com.quicksmart.android.consumer_activities.adapter_model

import com.google.firebase.Timestamp

/**
 * Represents a single ride (instant ride) booking.
 *
 * @param bookingId      Unique booking identifier
 * @param riderName      Name of the assigned rider/driver
 * @param vehicleType    "bike" or "car"
 * @param vehicleNo      Vehicle registration number
 * @param fromAddress    Pickup location address
 * @param toAddress      Destination address
 * @param fare           Estimated / confirmed fare string (e.g. "₹ 85")
 * @param status         "ongoing" | "upcoming" | "completed"
 * @param bookingDate    Firestore Timestamp of the booking
 * @param distanceLeft   Distance remaining to destination in km (for ongoing rides only)
 * @param isAtDestination Whether the rider has reached the destination
 * @param transactionId  ID of the associated payment transaction
 */
data class RideBookingModel(
    val bookingId        : String  = "",
    val consumerId       : String  = "",
    val providerId       : String  = "",
    val riderName        : String  = "",   // provider name
    val vehicleType      : String  = "",
    val vehicleNo        : String  = "",
    val fromAddress      : String  = "",   // consumer pickup
    val fromLat          : Double  = 0.0,
    val fromLng          : Double  = 0.0,
    val toAddress        : String  = "",   // provider's destination
    val toLat            : Double  = 0.0,
    val toLng            : Double  = 0.0,
    val providerLat      : Double  = 0.0,
    val providerLng      : Double  = 0.0,
    val totalPersons     : Int     = 1,
    val perPersonPrice   : Int     = 0,
    val fare             : String  = "",   // formatted total = perPersonPrice * totalPersons
    val status           : String  = "pending",  // pending | accepted | picked_up | completed | cancelled | rejected
    val bookingType      : String  = "ride",
    val bookingDate      : Timestamp? = null,
    val distanceLeft     : Double  = 0.0,
    val isAtDestination  : Boolean = false,
    val paymentDone      : Boolean = false,  // set to true after Cashfree payment succeeds
    val transactionId    : String  = ""
)
