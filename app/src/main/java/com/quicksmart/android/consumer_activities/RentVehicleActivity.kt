package com.quicksmart.android.consumer_activities

import android.location.Geocoder
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.app
import com.google.firebase.firestore.FirebaseFirestore
import com.quicksmart.android.R
import com.quicksmart.android.consumer_activities.adapter_model.RentalVehicleAdapter
import com.quicksmart.android.consumer_activities.adapter_model.RentalVehicleModel
import com.quicksmart.android.reusable_methods.daysBetween
import com.quicksmart.android.reusable_methods.getString
import com.quicksmart.android.reusable_methods.showDateTimePicker
import com.quicksmart.android.reusable_methods.showToast
import java.util.Calendar
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class RentVehicleActivity : AppCompatActivity() {

    // ── Firestore ────────────────────────────────────────────────────────
    private val db = FirebaseFirestore.getInstance(Firebase.app, "quicksmart-db")

    // ── Vehicle type passed from HomeFragment ("bike" | "car") ────────────
    private var vehicleType: String = "car"

    // ── Selected date Calendars (needed for day calculation) ──────────────
    private var pickupCalendar: Calendar? = null
    private var returnCalendar: Calendar? = null

    // ── Calculated distance in km (haversine between geocoded locations) ──
    private var calculatedDistanceKm: Double = 0.0

    // ── Views ─────────────────────────────────────────────────────────────
    private lateinit var backButton           : ImageView
    private lateinit var vehicleTypeIcon      : ImageView
    private lateinit var vehicleTypeLabel     : TextView
    private lateinit var pickupLocationInput  : EditText
    private lateinit var dropoffLocationInput : EditText
    private lateinit var pickupDateTimeInput  : EditText
    private lateinit var returnDateTimeInput  : EditText
    private lateinit var passengerColumn      : LinearLayout
    private lateinit var passengerCountInput  : EditText
    private lateinit var bikePassengerNote    : TextView
    private lateinit var rentalDaysInput      : EditText
    private lateinit var luggageSection       : LinearLayout
    private lateinit var luggageCountInput    : EditText
    private lateinit var luggageNote          : TextView
    private lateinit var driverRow            : LinearLayout
    private lateinit var driverRequiredSwitch : MaterialSwitch
    private lateinit var contactNumberInput   : EditText
    private lateinit var specialRequestsInput : EditText
    private lateinit var showVehiclesButton   : Button

    // ─────────────────────────────────────────────────────────────────────
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rent_vehicle)

        vehicleType = intent.getStringExtra("vehicle_type") ?: "car"

        bindViews()
        applyVehicleTypeRules()
        autoFillContactNumber()
        setupListeners()
    }

    // ─────────────────────────────────────────────────────────────────────
    // Bind
    // ─────────────────────────────────────────────────────────────────────
    private fun bindViews() {
        backButton            = findViewById(R.id.backButton)
        vehicleTypeIcon       = findViewById(R.id.vehicleTypeIcon)
        vehicleTypeLabel      = findViewById(R.id.vehicleTypeLabel)
        pickupLocationInput   = findViewById(R.id.pickupLocationInput)
        dropoffLocationInput  = findViewById(R.id.dropoffLocationInput)
        pickupDateTimeInput   = findViewById(R.id.pickupDateTimeInput)
        returnDateTimeInput   = findViewById(R.id.returnDateTimeInput)
        passengerColumn       = findViewById(R.id.passengerColumn)
        passengerCountInput   = findViewById(R.id.passengerCountInput)
        bikePassengerNote     = findViewById(R.id.bikePassengerNote)
        rentalDaysInput       = findViewById(R.id.rentalDaysInput)
        luggageSection        = findViewById(R.id.luggageSection)
        luggageCountInput     = findViewById(R.id.luggageCountInput)
        luggageNote           = findViewById(R.id.luggageNote)
        driverRow             = findViewById(R.id.driverRow)
        driverRequiredSwitch  = findViewById(R.id.driverRequiredSwitch)
        contactNumberInput    = findViewById(R.id.contactNumberInput)
        specialRequestsInput  = findViewById(R.id.specialRequestsInput)
        showVehiclesButton    = findViewById(R.id.showVehiclesButton)
    }

    // ─────────────────────────────────────────────────────────────────────
    // Auto-fill contact number from SharedPreferences
    // ─────────────────────────────────────────────────────────────────────
    private fun autoFillContactNumber() {
        val mobile = getString("mobile")   // stored key in SharedPreferences
        if (mobile.isNotBlank()) {
            contactNumberInput.setText(mobile)
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Vehicle-type rules — called once after bindViews()
    // ─────────────────────────────────────────────────────────────────────
    private fun applyVehicleTypeRules() {
        if (vehicleType == "bike") {
            vehicleTypeIcon.setImageResource(R.drawable.ic_bike)
            vehicleTypeLabel.text = "Bike / Scooter"
        } else {
            vehicleTypeIcon.setImageResource(R.drawable.ic_car)
            vehicleTypeLabel.text = "Car"
        }

        if (vehicleType == "bike") {
            passengerColumn.visibility   = View.GONE
            luggageSection.visibility    = View.GONE
            driverRow.visibility         = View.GONE
        } else {
            passengerColumn.visibility   = View.VISIBLE
            luggageSection.visibility    = View.VISIBLE
            driverRow.visibility         = View.VISIBLE
            bikePassengerNote.visibility = View.GONE

            passengerCountInput.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    val value = s?.toString()?.toIntOrNull() ?: 0
                    if (value > 20) {
                        passengerCountInput.removeTextChangedListener(this)
                        passengerCountInput.setText("20")
                        passengerCountInput.setSelection(2)
                        passengerCountInput.addTextChangedListener(this)
                        showToast("Maximum 20 passengers allowed.")
                    }
                }
            })

            luggageNote.text       = "Max 10 bags allowed for car"
            luggageNote.visibility = View.VISIBLE
            luggageCountInput.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    val value = s?.toString()?.toIntOrNull() ?: 0
                    if (value > 10) {
                        luggageCountInput.removeTextChangedListener(this)
                        luggageCountInput.setText("10")
                        luggageCountInput.setSelection(2)
                        luggageCountInput.addTextChangedListener(this)
                        showToast("Maximum 10 bags allowed for a car.")
                    }
                }
            })
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Listeners
    // ─────────────────────────────────────────────────────────────────────
    private fun setupListeners() {
        backButton.setOnClickListener { finish() }

        val maxMs = System.currentTimeMillis() + (7L * 24 * 60 * 60 * 1000)

        pickupDateTimeInput.setOnClickListener {
            showDateTimePicker(
                minDateMillis = System.currentTimeMillis() - 1000,
                maxDateMillis = maxMs
            ) { formatted, cal ->
                pickupDateTimeInput.setText(formatted)
                pickupCalendar = cal
                recalculateDays()
            }
        }

        returnDateTimeInput.setOnClickListener {
            val minMs = pickupCalendar?.timeInMillis ?: (System.currentTimeMillis() - 1000)
            showDateTimePicker(
                minDateMillis = minMs,
                maxDateMillis = maxMs
            ) { formatted, cal ->
                returnDateTimeInput.setText(formatted)
                returnCalendar = cal
                recalculateDays()
            }
        }

        showVehiclesButton.setOnClickListener {
            if (validateForm()) geocodeAndShowSheet()
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Auto-calculate rental days whenever both dates are set
    // ─────────────────────────────────────────────────────────────────────
    private fun recalculateDays() {
        val from = pickupCalendar ?: return
        val to   = returnCalendar ?: return

        if (to.before(from) || to == from) {
            showToast("Return date must be after pickup date.")
            returnDateTimeInput.text.clear()
            returnCalendar = null
            rentalDaysInput.setText("")
            return
        }

        val days = daysBetween(from, to)
        rentalDaysInput.setText("$days")
    }

    // ─────────────────────────────────────────────────────────────────────
    // Form Validation
    // ─────────────────────────────────────────────────────────────────────
    private fun validateForm(): Boolean {
        if (pickupLocationInput.text.isBlank()) {
            showToast("Please enter a pickup location.")
            pickupLocationInput.requestFocus(); return false
        }
        if (dropoffLocationInput.text.isBlank()) {
            showToast("Please enter a drop-off location.")
            dropoffLocationInput.requestFocus(); return false
        }
        if (pickupDateTimeInput.text.isBlank()) {
            showToast("Please select a pickup date & time."); return false
        }
        if (returnDateTimeInput.text.isBlank()) {
            showToast("Please select a return date & time."); return false
        }
        if (vehicleType == "car" && passengerCountInput.text.isBlank()) {
            showToast("Please enter the number of passengers.")
            passengerCountInput.requestFocus(); return false
        }
        if (contactNumberInput.text.isBlank()) {
            showToast("Please enter a contact number.")
            contactNumberInput.requestFocus(); return false
        }
        return true
    }

    // ─────────────────────────────────────────────────────────────────────
    // Geocode addresses and compute distance, then show the vehicle sheet
    // ─────────────────────────────────────────────────────────────────────
    @Suppress("DEPRECATION")
    private fun geocodeAndShowSheet() {
        showVehiclesButton.isEnabled = false
        showVehiclesButton.text      = "Calculating distance…"

        val geocoder  = Geocoder(this, Locale.getDefault())
        val pickup    = pickupLocationInput.text.toString().trim()
        val dropoff   = dropoffLocationInput.text.toString().trim()

        try {
            val pickupResults  = geocoder.getFromLocationName(pickup, 1)
            val dropoffResults = geocoder.getFromLocationName(dropoff, 1)

            if (pickupResults.isNullOrEmpty() || dropoffResults.isNullOrEmpty()) {
                showToast("Could not geocode one or both locations. Check address names.")
                showVehiclesButton.isEnabled = true
                showVehiclesButton.text      = "Show Available Vehicles"
                return
            }

            val pLat = pickupResults[0].latitude
            val pLng = pickupResults[0].longitude
            val dLat = dropoffResults[0].latitude
            val dLng = dropoffResults[0].longitude

            calculatedDistanceKm = haversineKm(pLat, pLng, dLat, dLng)
        } catch (e: Exception) {
            // If geocoding fails (no internet etc.) fall back to 0 km
            calculatedDistanceKm = 0.0
        }

        showVehiclesButton.isEnabled = true
        showVehiclesButton.text      = "Show Available Vehicles"
        showVehicleSheet()
    }

    // ─────────────────────────────────────────────────────────────────────
    // Haversine formula — returns air-distance in km
    // ─────────────────────────────────────────────────────────────────────
    private fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r     = 6371.0 // Earth radius km
        val dLat  = Math.toRadians(lat2 - lat1)
        val dLon  = Math.toRadians(lon2 - lon1)
        val a     = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        val c     = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    // ─────────────────────────────────────────────────────────────────────
    // Rental vehicle picker bottom sheet
    // ─────────────────────────────────────────────────────────────────────
    private fun showVehicleSheet() {
        val sheet     = BottomSheetDialog(this)
        val sheetView = layoutInflater.inflate(R.layout.bottom_sheet_rental_vehicle_list, null)
        sheet.setContentView(sheetView)

        val peekPx = (340 * resources.displayMetrics.density).toInt()
        sheet.behavior.peekHeight = peekPx

        val sheetTitle       = sheetView.findViewById<TextView>(R.id.rentalSheetTitle)
        val perKmNotice      = sheetView.findViewById<TextView>(R.id.perKmRateNotice)
        val noVehiclesMsg    = sheetView.findViewById<TextView>(R.id.noVehiclesMessage)
        val vehiclesList     = sheetView.findViewById<RecyclerView>(R.id.rentalVehiclesList)
        val sheetProgress    = sheetView.findViewById<ProgressBar?>(R.id.sheetProgressBar)

        val days      = rentalDaysInput.text.toString().ifBlank { "1" }
        val typeLabel = if (vehicleType == "bike") "Bike / Scooter" else "Car"
        val daysLabel = if (days == "1") "1 day" else "$days days"
        val distLabel = if (calculatedDistanceKm > 0)
            " · ~${String.format("%.1f", calculatedDistanceKm)} km"
        else ""
        sheetTitle.text = "Available $typeLabel for Rent ($daysLabel$distLabel)"

        if (calculatedDistanceKm > 0) {
            perKmNotice.text = "Fare = distance (${String.format("%.1f", calculatedDistanceKm)} km) × 2 × rate/km"
        }

        sheetProgress?.visibility = View.VISIBLE
        noVehiclesMsg.visibility  = View.GONE
        vehiclesList.visibility   = View.GONE

        // ── Fetch overlapping bookings first ──────────────────────────────
        db.collection("bookings")
            .whereEqualTo("bookingType", "rental")
            .whereEqualTo("status", "accepted")
            .get()
            .addOnSuccessListener { bookingSnapshot ->
                val bookedVehicleIds = mutableSetOf<String>()
                val userFrom = Timestamp(pickupCalendar!!.time)
                val userTo   = Timestamp(returnCalendar!!.time)

                bookingSnapshot.documents.forEach { bDoc ->
                    val bFrom = bDoc.getTimestamp("fromDate")
                    val bTo   = bDoc.getTimestamp("toDate")
                    val bVId  = bDoc.getString("vehicleId")
                    if (bFrom != null && bTo != null && bVId != null) {
                        if (bFrom.seconds <= userTo.seconds && bTo.seconds >= userFrom.seconds) {
                            bookedVehicleIds.add(bVId)
                        }
                    }
                }

                // ── Fetch approved vehicles, filter booked ones ───────────
                db.collection("vehicles")
                    .whereEqualTo("purpose", "For Rent")
                    .whereEqualTo("status", "approved")
                    .whereEqualTo("vehicleType", if (vehicleType == "bike") "Bike / Scooter" else "Car")
                    .get()
                    .addOnSuccessListener { snapshot ->
                        sheetProgress?.visibility = View.GONE
                        val pricePerKm = 0.0
                        val fetchedVehicles = snapshot.documents
                            .map { doc ->
                                val rate = doc.getDouble("pricePerKm") ?: 0.0
                                RentalVehicleModel(
                                    vehicleId   = doc.id,
                                    ownerName   = doc.getString("ownerName") ?: "Unknown",
                                    ownerId     = doc.getString("ownerId") ?: "",
                                    vehicleType = vehicleType,
                                    vehicleNo   = doc.getString("vehicleNumber") ?: "",
                                    perKmRate   = "₹${rate.toInt()}"
                                )
                            }
                            .filter { it.vehicleId !in bookedVehicleIds }

                        if (fetchedVehicles.isEmpty()) {
                            noVehiclesMsg.visibility = View.VISIBLE
                            vehiclesList.visibility  = View.GONE
                            perKmNotice.visibility   = View.GONE
                        } else {
                            noVehiclesMsg.visibility = View.GONE
                            vehiclesList.visibility  = View.VISIBLE
                            perKmNotice.visibility   = View.VISIBLE

                            val adapter = RentalVehicleAdapter(fetchedVehicles) { selected ->
                                sheet.dismiss()
                                confirmAndSubmitBooking(selected)
                            }
                            vehiclesList.layoutManager = LinearLayoutManager(this)
                            vehiclesList.adapter = adapter
                        }
                    }
                    .addOnFailureListener { e ->
                        sheetProgress?.visibility = View.GONE
                        showToast("Failed to fetch vehicles: ${e.message}")
                    }
            }
            .addOnFailureListener { e ->
                sheetProgress?.visibility = View.GONE
                showToast("Failed to check bookings: ${e.message}")
            }

        sheet.show()
    }

    // ─────────────────────────────────────────────────────────────────────
    // Final booking submission
    // ─────────────────────────────────────────────────────────────────────
    private fun confirmAndSubmitBooking(vehicle: RentalVehicleModel) {
        val userId    = getString("userId")
        val firstName = getString("firstName")
        val lastName  = getString("lastName")
        val fullName  = "$firstName $lastName".trim()

        if (userId.isEmpty()) {
            showToast("Session expired. Please login again.")
            return
        }

        val days     = rentalDaysInput.text.toString().toIntOrNull() ?: 1
        val rateInt  = vehicle.perKmRate.replace("₹", "").trim().toIntOrNull() ?: 0

        // Amount = distance (round-trip × 2) × per-km rate
        val totalFare = if (calculatedDistanceKm > 0) {
            val roundTrip = calculatedDistanceKm * 2
            (roundTrip * rateInt).toLong()
        } else 0L

        val amountStr = if (totalFare > 0) "₹$totalFare (est.)" else "${vehicle.perKmRate}/km"

        val bookingDoc = hashMapOf(
            "consumerId"      to userId,
            "consumerName"    to fullName,
            "providerId"         to vehicle.ownerId,
            "vehicleId"       to vehicle.vehicleId,
            "providerName"       to vehicle.ownerName,
            "vehicleNo"       to vehicle.vehicleNo,
            "vehicleType"     to vehicle.vehicleType,
            "pickupLocation"  to pickupLocationInput.text.toString().trim(),
            "dropoffLocation" to dropoffLocationInput.text.toString().trim(),
            "distanceKm"      to String.format("%.1f", calculatedDistanceKm),
            "fromDate"        to Timestamp(pickupCalendar!!.time),
            "toDate"          to Timestamp(returnCalendar!!.time),
            "tripDays"        to (rentalDaysInput.text.toString() + if (days == 1) " day" else " days"),
            "amount"          to amountStr,
            "contactNumber"   to contactNumberInput.text.toString().trim(),
            "specialRequests" to specialRequestsInput.text.toString().trim(),
            "passengerCount"  to (passengerCountInput.text.toString().ifBlank { "1" }),
            "luggageCount"    to (luggageCountInput.text.toString().ifBlank { "0" }),
            "withDriver"      to driverRequiredSwitch.isChecked,
            "status"          to "pending",
            "bookingType"     to "rental",
            "bookingDate"     to Timestamp.now()
        )

        showVehiclesButton.isEnabled = false
        showVehiclesButton.text      = "Sending Request…"

        db.collection("bookings")
            .add(bookingDoc)
            .addOnSuccessListener {
                showToast("Rental request sent successfully to the provider!")
                finish()
            }
            .addOnFailureListener { e ->
                showVehiclesButton.isEnabled = true
                showVehiclesButton.text      = "Show Available Vehicles"
                showToast("Failed to book: ${e.message}")
            }
    }
}
